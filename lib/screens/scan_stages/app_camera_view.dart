import 'dart:async';
import 'dart:developer' as developer;

import 'package:camera/camera.dart';
import 'package:cropdoc/themes/app_colors.dart';
import 'package:flutter/material.dart';

class AppCameraView extends StatefulWidget {
  final VoidCallback onPermissionDenied;

  const AppCameraView({super.key, required this.onPermissionDenied});

  @override
  State<AppCameraView> createState() => AppCameraViewState();
}

class AppCameraViewState extends State<AppCameraView> {
  CameraController? _controller;
  bool _isLoading = true;
  bool _flashEnabled = false;

  bool get isReady => _controller?.value.isInitialized ?? false;
  bool get isFlashEnabled => _flashEnabled;

  @override
  void initState() {
    super.initState();
    _initializeCamera();
  }

  Future<void> _initializeCamera() async {
    try {
      final cameras = await availableCameras();
      if (!mounted || cameras.isEmpty) {
        setState(() => _isLoading = false);
        return;
      }

      final selectedCamera = cameras.firstWhere(
        (camera) => camera.lensDirection == CameraLensDirection.back,
        orElse: () => cameras.first,
      );

      final controller = CameraController(
        selectedCamera,
        ResolutionPreset.high,
        enableAudio: false,
      );

      await controller.initialize();
      if (!mounted) {
        await controller.dispose();
        return;
      }

      setState(() {
        _controller = controller;
        _isLoading = false;
      });
    } on CameraException catch (error, stackTrace) {
      developer.log(
        'Could not initialize in-app camera preview.',
        error: error,
        stackTrace: stackTrace,
      );
      if (!mounted) return;
      setState(() => _isLoading = false);
      if (error.code.toLowerCase().contains('denied')) {
        widget.onPermissionDenied();
      }
    } catch (error, stackTrace) {
      developer.log(
        'Could not initialize in-app camera preview.',
        error: error,
        stackTrace: stackTrace,
      );
      if (!mounted) return;
      setState(() => _isLoading = false);
    }
  }

  Future<String?> captureImage() async {
    final controller = _controller;
    if (controller == null ||
        !controller.value.isInitialized ||
        controller.value.isTakingPicture) {
      return null;
    }

    try {
      await turnOffFlash();
      final shot = await controller.takePicture();
      return shot.path;
    } catch (error, stackTrace) {
      developer.log(
        'Could not capture image from in-app camera preview.',
        error: error,
        stackTrace: stackTrace,
      );
      return null;
    }
  }

  Future<bool> toggleFlash() async {
    final controller = _controller;
    if (controller == null || !controller.value.isInitialized) return false;

    final nextFlashEnabled = !_flashEnabled;
    try {
      await controller.setFlashMode(
        nextFlashEnabled ? FlashMode.torch : FlashMode.off,
      );
      if (!mounted) return false;
      setState(() => _flashEnabled = nextFlashEnabled);
      return true;
    } catch (error, stackTrace) {
      developer.log(
        'Could not change camera flash mode.',
        error: error,
        stackTrace: stackTrace,
      );
      return false;
    }
  }

  Future<void> turnOffFlash() async {
    final controller = _controller;
    if (!_flashEnabled ||
        controller == null ||
        !controller.value.isInitialized) {
      return;
    }

    try {
      await controller.setFlashMode(FlashMode.off);
      _flashEnabled = false;
    } catch (error, stackTrace) {
      developer.log(
        'Could not turn off camera flash.',
        error: error,
        stackTrace: stackTrace,
      );
    }
  }

  @override
  void dispose() {
    final controller = _controller;
    _controller = null;
    if (controller != null) {
      unawaited(_disposeController(controller));
    }
    super.dispose();
  }

  Future<void> _disposeController(CameraController controller) async {
    try {
      if (controller.value.isInitialized) {
        await controller.setFlashMode(FlashMode.off);
      }
    } catch (error, stackTrace) {
      developer.log(
        'Could not turn off camera flash while leaving scan.',
        error: error,
        stackTrace: stackTrace,
      );
    }

    try {
      await controller.dispose();
    } catch (error, stackTrace) {
      developer.log(
        'Could not dispose the in-app camera controller.',
        error: error,
        stackTrace: stackTrace,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const ColoredBox(
        color: AppColors.primary,
        child: Center(
          child: CircularProgressIndicator(color: AppColors.secondary),
        ),
      );
    }

    final controller = _controller;
    if (controller == null || !controller.value.isInitialized) {
      return const ColoredBox(color: AppColors.primary);
    }

    return Stack(
      fit: StackFit.expand,
      children: [
        ClipRect(
          child: FittedBox(
            fit: BoxFit.cover,
            child: SizedBox(
              width: controller.value.previewSize!.height,
              height: controller.value.previewSize!.width,
              child: CameraPreview(controller),
            ),
          ),
        ), 
        ColoredBox(color: AppColors.primary.withValues(alpha: .18)),
        const CustomPaint(painter: _CameraGridPainter()),
      ],
    );
  }
}

class _CameraGridPainter extends CustomPainter {
  const _CameraGridPainter();

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = AppColors.secondary.withValues(alpha: .28)
      ..strokeWidth = 1;

    for (var division = 1; division < 3; division++) {
      final x = size.width * division / 3;
      final y = size.height * division / 3;
      canvas.drawLine(Offset(x, 0), Offset(x, size.height), paint);
      canvas.drawLine(Offset(0, y), Offset(size.width, y), paint);
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
