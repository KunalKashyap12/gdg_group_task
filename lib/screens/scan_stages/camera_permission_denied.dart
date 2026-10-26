import 'package:cropdoc/themes/app_colors.dart';
import 'package:cropdoc/themes/app_fonts.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

class CameraPermissionDenied extends StatelessWidget {
  final VoidCallback onBack;
  final VoidCallback onOpenSettings;
  final VoidCallback onChooseGallery;
  final VoidCallback onRetryCamera;

  const CameraPermissionDenied({
    super.key,
    required this.onBack,
    required this.onOpenSettings,
    required this.onChooseGallery,
    required this.onRetryCamera,
  });

  @override
  Widget build(BuildContext context) => ColoredBox(
    color: AppColors.background,
    child: Stack(
      children: [
        Positioned(
          top: 16,
          right: -28,
          child: Opacity(
            opacity: 0.48,
            child: SvgPicture.asset(
              'assets/svg/Leaf 3.svg',
              width: 130,
              height: 180,
              colorFilter: const ColorFilter.mode(
                AppColors.leaf,
                BlendMode.srcIn,
              ),
            ),
          ),
        ),
        Positioned(
          bottom: 0,
          left: -45,
          child: SvgPicture.asset(
            'assets/svg/Leaf 6.svg',
            width: 150,
            height: 210,
            colorFilter: const ColorFilter.mode(
              Color(0xFF21825C),
              BlendMode.srcIn,
            ),
          ),
        ),
        SafeArea(
          child: Column(
            children: [
              Align(
                alignment: Alignment.centerLeft,
                child: IconButton(
                  tooltip: 'Back',
                  onPressed: onBack,
                  icon: const Icon(Icons.arrow_back_rounded),
                  color: AppColors.primary,
                ),
              ),
              const Spacer(),
              const Icon(
                Icons.no_photography_outlined,
                size: 64,
                color: AppColors.primary,
              ),
              const SizedBox(height: 18),
              Text(
                'Camera / Gallery\nPermission Denied',
                textAlign: TextAlign.center,
                style: AppFonts.screenTitle.copyWith(
                  color: AppColors.primary,
                  fontSize: 22,
                  height: 1.2,
                ),
              ),
              const SizedBox(height: 14),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 36),
                child: Text(
                  'CropDoc needs camera access to scan and analyze crop leaves. Allow access in your device settings, or choose a leaf photo from your gallery.',
                  textAlign: TextAlign.center,
                  style: AppFonts.caption.copyWith(
                    color: AppColors.muted,
                    fontSize: 14,
                    height: 1.7,
                  ),
                ),
              ),
              const SizedBox(height: 28),
              SizedBox(
                width: 210,
                height: 50,
                child: FilledButton.icon(
                  onPressed: onOpenSettings,
                  style: FilledButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    foregroundColor: AppColors.secondary,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(14),
                    ),
                  ),
                  icon: const Icon(Icons.settings_outlined),
                  label: const Text('Open settings'),
                ),
              ),
              const SizedBox(height: 8),
              TextButton(
                onPressed: onChooseGallery,
                child: Text(
                  'Choose from gallery instead',
                  style: AppFonts.caption.copyWith(
                    color: AppColors.primary,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              TextButton(
                onPressed: onRetryCamera,
                child: Text(
                  'Try camera again',
                  style: AppFonts.caption.copyWith(
                    color: AppColors.primary,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              const Spacer(),
              const SizedBox(height: 16),
            ],
          ),
        ),
      ],
    ),
  );
}
