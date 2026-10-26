import 'dart:developer' as developer;

import 'package:cropdoc/screens/scan_stages/camera_permission_denied.dart';
import 'package:cropdoc/screens/scan_stages/analyzing_stage.dart';
import 'package:cropdoc/screens/scan_stages/preview_stage.dart';
import 'package:cropdoc/screens/scan_stages/results_stage.dart';
import 'package:cropdoc/screens/scan_stages/upload_stage.dart';
import 'package:cropdoc/services/api_client.dart';
import 'package:cropdoc/services/auth_service.dart';
import 'package:cropdoc/themes/app_colors.dart';
import 'package:cropdoc/themes/app_fonts.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

class ScanFlow extends StatefulWidget {
  final VoidCallback onClose;
  const ScanFlow({super.key, required this.onClose});

  @override
  State<ScanFlow> createState() => _ScanFlowState();
}

class _ScanFlowState extends State<ScanFlow> {
  static const _settingsChannel = MethodChannel('cropdoc/app_settings');
  final ImagePicker _picker = ImagePicker();
  bool _cameraPermissionDenied = false;
  bool _isOpeningSettings = false;

  Future<void> _openAppSettings() async {
    if (_isOpeningSettings) return;
    setState(() => _isOpeningSettings = true);
    try {
      final opened = await _settingsChannel.invokeMethod<bool>(
        'openAppSettings',
      );
      if (!mounted) return;
      if (opened != true) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Could not open device settings.')),
        );
      }
    } on PlatformException catch (error) {
      developer.log(
        'Could not open device settings.',
        error: error,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Could not open device settings.')),
      );
    } finally {
      if (mounted) setState(() => _isOpeningSettings = false);
    }
  }

  Future<void> _openPreview(String imagePath) async {
    if (!mounted) return;
    await Navigator.of(context).push<void>(
      MaterialPageRoute(
        builder: (context) => _PreviewPage(imagePath: imagePath),
      ),
    );
  }

  Future<void> _pickFromGallery() async {
    try {
      final image = await _picker.pickImage(
        source: ImageSource.gallery,
        imageQuality: 85,
        maxWidth: 1600,
      );
      if (image == null) return;
      await _openPreview(image.path);
    } catch (error, stackTrace) {
      developer.log(
        'Could not select image from gallery.',
        error: error,
        stackTrace: stackTrace,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Could not open gallery. Please try again.'),
        ),
      );
    }
  }

  Future<void> _captureWithSystemCamera() async {
    try {
      final image = await _picker.pickImage(
        source: ImageSource.camera,
        imageQuality: 85,
        maxWidth: 1600,
      );
      if (image == null) return;
      await _openPreview(image.path);
    } catch (error, stackTrace) {
      developer.log(
        'Could not capture image from system camera.',
        error: error,
        stackTrace: stackTrace,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Could not open camera. Please try again.'),
        ),
      );
    }
  }

  void _showHelp() {
    showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: AppColors.surface,
        title: const Text('Scan a plant'),
        content: const Text(
          'Center one leaf in the frame and use a clear, well-lit photo. You can also choose an image from your gallery.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Got it'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (_cameraPermissionDenied) {
      return Padding(
        padding: const EdgeInsets.only(bottom: 90),
        child: CameraPermissionDenied(
          onBack: widget.onClose,
          onOpenSettings: _openAppSettings,
          onChooseGallery: _pickFromGallery,
          onRetryCamera: () {
            setState(() => _cameraPermissionDenied = false);
          },
        ),
      );
    }

    return Scaffold(
      backgroundColor: AppColors.primary,
      appBar: AppBar(
        backgroundColor: AppColors.primary,
        foregroundColor: AppColors.secondary,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded),
          onPressed: widget.onClose,
        ),
        title: Text(
          'Scan Plant',
          style: AppFonts.button.copyWith(
            color: AppColors.secondary,
            fontSize: 16,
          ),
        ),
        centerTitle: true,
        actions: [
          IconButton(
            tooltip: 'Scan help',
            onPressed: _showHelp,
            icon: const Icon(Icons.help_rounded),
          ),
        ],
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.only(bottom: 90),
          child: AnimatedSwitcher(
            duration: const Duration(milliseconds: 200),
            child: UploadStage(
              key: const ValueKey('upload-stage'),
              onGallery: _pickFromGallery,
              onCameraFallback: _captureWithSystemCamera,
              onCameraCaptured: _openPreview,
              onCameraPermissionDenied: () {
                if (mounted && !_cameraPermissionDenied) {
                  setState(() => _cameraPermissionDenied = true);
                }
              },
            ),
          ),
        ),
      ),
    );
  }
}

class _PreviewPage extends StatelessWidget {
  final String imagePath;

  const _PreviewPage({required this.imagePath});

  @override
  Widget build(BuildContext context) => Scaffold(
    backgroundColor: AppColors.background,
    appBar: AppBar(
      backgroundColor: AppColors.background,
      foregroundColor: AppColors.primary,
      title: Text(
        'Preview image',
        style: AppFonts.button.copyWith(color: AppColors.primary, fontSize: 16),
      ),
      centerTitle: true,
    ),
    body: PreviewStage(
      imagePath: imagePath,
      onRetake: () => Navigator.of(context).pop(),
      onContinue: () {
        Navigator.of(context).pushReplacement<void, void>(
          MaterialPageRoute<void>(
            builder: (context) => _AnalyzingPage(imagePath: imagePath),
          ),
        );
      },
    ),
  );
}

class _AnalyzingPage extends ConsumerStatefulWidget {
  final String imagePath;

  const _AnalyzingPage({required this.imagePath});

  @override
  ConsumerState<_AnalyzingPage> createState() => _AnalyzingPageState();
}

class _AnalyzingPageState extends ConsumerState<_AnalyzingPage> {
  String? _error;
  bool _isSubmitting = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _submitPrediction());
  }

  Future<void> _submitPrediction() async {
    if (_isSubmitting) return;
    setState(() {
      _isSubmitting = true;
      _error = null;
    });
    try {
      final response = await ref
          .read(apiClientProvider)
          .createPrediction(imagePath: widget.imagePath);
      if (response is! Map) {
        throw const FormatException(
          'The server returned an invalid prediction response.',
        );
      }
      final prediction = Map<String, Object?>.from(response);
      if (!mounted) return;
      await Navigator.of(context).pushReplacement<void, void>(
        MaterialPageRoute<void>(
          builder: (context) => _ResultsPage(
            imagePath: widget.imagePath,
            prediction: prediction,
          ),
        ),
      );
    } catch (error, stackTrace) {
      developer.log(
        'Failed to analyze the selected crop image.',
        error: error,
        stackTrace: stackTrace,
      );
      if (mounted) {
        setState(() {
          _error = error is FormatException
              ? error.message
              : error is ApiException
              ? error.message
              : 'Could not analyze the image. Check your connection and try again.';
        });
      }
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    backgroundColor: AppColors.successSurface,
    appBar: AppBar(
      backgroundColor: AppColors.background,
      foregroundColor: AppColors.primary,
      leading: _error == null
          ? null
          : IconButton(
              onPressed: () => Navigator.of(context).pop(),
              icon: const Icon(Icons.arrow_back_rounded),
            ),
      title: Text(
        'Crop check',
        style: AppFonts.button.copyWith(color: AppColors.primary, fontSize: 16),
      ),
      centerTitle: true,
    ),
    body: _error == null
        ? const AnalyzingStage()
        : Center(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(
                    Icons.cloud_off_rounded,
                    color: AppColors.error,
                    size: 44,
                  ),
                  const SizedBox(height: 12),
                  Text(
                    _error!,
                    textAlign: TextAlign.center,
                    style: AppFonts.caption.copyWith(
                      color: AppColors.primary,
                      fontSize: 15,
                    ),
                  ),
                  const SizedBox(height: 16),
                  ElevatedButton(
                    onPressed: _isSubmitting ? null : _submitPrediction,
                    child: const Text('Retry'),
                  ),
                ],
              ),
            ),
          ),
  );
}

class _ResultsPage extends StatelessWidget {
  final String imagePath;
  final Map<String, Object?> prediction;

  const _ResultsPage({
    required this.imagePath,
    required this.prediction,
  });

  @override
  Widget build(BuildContext context) => Scaffold(
    backgroundColor: AppColors.background,
    appBar: AppBar(
      backgroundColor: AppColors.background,
      foregroundColor: AppColors.primary,
      leading: IconButton(
        tooltip: 'Back',
        onPressed: () => Navigator.of(context).pop(),
        icon: const Icon(Icons.arrow_back_rounded),
      ),
      title: Text(
        'Report',
        style: AppFonts.button.copyWith(color: AppColors.primary, fontSize: 16),
      ),
      centerTitle: true,
    ),
    body: ResultsStage(imagePath: imagePath, prediction: prediction),
  );
}
