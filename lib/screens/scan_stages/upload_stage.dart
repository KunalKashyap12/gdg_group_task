import 'package:cropdoc/screens/scan_stages/app_camera_view.dart';
import 'package:cropdoc/themes/app_colors.dart';
import 'package:cropdoc/themes/app_fonts.dart';
import 'package:flutter/material.dart';

class UploadStage extends StatefulWidget {
  final Future<void> Function(String imagePath) onCameraCaptured;
  final Future<void> Function() onCameraFallback;
  final Future<void> Function() onGallery;
  final VoidCallback onCameraPermissionDenied;

  const UploadStage({
    super.key,
    required this.onCameraCaptured,
    required this.onCameraFallback,
    required this.onGallery,
    required this.onCameraPermissionDenied,
  });

  @override
  State<UploadStage> createState() => _UploadStageState();
}

class _UploadStageState extends State<UploadStage> {
  static const _previewImages = [
    'https://images.unsplash.com/photo-1497250681960-ef046c08a56e?auto=format&fit=crop&w=240&h=240&q=80',
    'https://images.unsplash.com/photo-1501004318641-b39e6451bec6?auto=format&fit=crop&w=240&h=240&q=80',
    'https://images.unsplash.com/photo-1523348837708-15d4a09cfac2?auto=format&fit=crop&w=240&h=240&q=80',
  ];

  final GlobalKey<AppCameraViewState> _cameraKey =
      GlobalKey<AppCameraViewState>();
  bool _isCapturing = false;

  Future<void> _handleCapture() async {
    if (_isCapturing) return;

    setState(() => _isCapturing = true);
    try {
      final cameraPath = await _cameraKey.currentState?.captureImage();
      if (!mounted) return;

      if (cameraPath != null) {
        await widget.onCameraCaptured(cameraPath);
      } else {
        await widget.onCameraFallback();
      }
    } finally {
      if (mounted) setState(() => _isCapturing = false);
    }
  }

  Future<void> _handleFlashToggle() async {
    final toggled = await _cameraKey.currentState?.toggleFlash() ?? false;
    if (!mounted) return;
    if (toggled) {
      setState(() {});
      return;
    }
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Flash is unavailable on this camera.')),
    );
  }

  Future<void> _turnOffFlash() async {
    await _cameraKey.currentState?.turnOffFlash();
    if (mounted) setState(() {});
  }

  Future<void> _handleGallery() async {
    await _turnOffFlash();
    if (mounted) await widget.onGallery();
  }

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.fromLTRB(8, 6, 8, 6),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Expanded(
          flex: 6,
          child: ClipRRect(
            borderRadius: BorderRadius.circular(22),
            child: Stack(
              fit: StackFit.expand,
              children: [
                AppCameraView(
                  key: _cameraKey,
                  onPermissionDenied: widget.onCameraPermissionDenied,
                ),
                Positioned.fill(
                  child: IgnorePointer(
                    child: DecoratedBox(
                      decoration: BoxDecoration(
                        border: Border.all(
                          color: AppColors.tertiary.withValues(alpha: .72),
                          width: 2,
                        ),
                        borderRadius: BorderRadius.circular(22),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 8),
        Text(
          'Preview images',
          style: AppFonts.button.copyWith(
            color: AppColors.secondary,
            fontSize: 14,
            fontWeight: FontWeight.w700,
          ),
        ),
        const SizedBox(height: 5),
        SizedBox(
          height: 58,
          child: Row(
            children: List.generate(3, (index) {
              return Expanded(
                child: Padding(
                  padding: EdgeInsets.only(right: index == 2 ? 0 : 12),
                  child: Container(
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(10),
                      border: Border.all(
                        color: AppColors.tertiary.withValues(alpha: .25),
                      ),
                    ),
                    clipBehavior: Clip.antiAlias,
                    child: Image.network(
                      _previewImages[index],
                      fit: BoxFit.cover,
                      errorBuilder: (context, error, stackTrace) =>
                          const ColoredBox(
                            color: AppColors.successSurface,
                            child: Icon(
                              Icons.eco_rounded,
                              color: AppColors.tertiary,
                            ),
                          ),
                    ),
                  ),
                ),
              );
            }),
          ),
        ),
        SizedBox(height: 12),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            IconButton(
              tooltip: 'Choose from gallery',
              onPressed: _isCapturing ? null : _handleGallery,
              icon: const Icon(Icons.image_rounded),
              color: AppColors.secondary,
              iconSize: 34,
            ),
            InkWell(
              onTap: _isCapturing ? null : _handleCapture,
              customBorder: const CircleBorder(),
              child: Container(
                width: 82,
                height: 82,
                padding: const EdgeInsets.all(6),
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  border: Border.all(color: AppColors.secondary, width: 2),
                ),
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    color: AppColors.secondary,
                    shape: BoxShape.circle,
                    border: Border.all(color: AppColors.errorSurface, width: 4),
                  ),
                  child: _isCapturing
                      ? const Padding(
                          padding: EdgeInsets.all(18),
                          child: CircularProgressIndicator(
                            color: AppColors.primary,
                            strokeWidth: 2.5,
                          ),
                        )
                      : null,
                ),
              ),
            ),
            IconButton(
              tooltip: 'Toggle flash',
              onPressed: _isCapturing ? null : _handleFlashToggle,
              icon: Icon(
                _cameraKey.currentState?.isFlashEnabled == true
                    ? Icons.flash_on_rounded
                    : Icons.flash_off_rounded,
              ),
              color: AppColors.secondary,
              iconSize: 34,
            ),
          ],
        ),
      ],
    ),
  );
}
