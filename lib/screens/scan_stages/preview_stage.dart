import 'dart:io';

import 'package:cropdoc/components/buttons.dart';
import 'package:cropdoc/themes/app_colors.dart';
import 'package:cropdoc/themes/app_fonts.dart';
import 'package:flutter/material.dart';

class PreviewStage extends StatelessWidget {
  final String? imagePath;
  final VoidCallback onRetake;
  final VoidCallback onContinue;

  const PreviewStage({
    super.key,
    required this.imagePath,
    required this.onRetake,
    required this.onContinue,
  });

  @override
  Widget build(BuildContext context) => SingleChildScrollView(
    padding: const EdgeInsets.fromLTRB(20, 18, 20, 28),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Container(
          height: 330,
          decoration: BoxDecoration(
            color: AppColors.primary,
            borderRadius: BorderRadius.circular(24),
          ),
          child: Stack(
            alignment: Alignment.center,
            children: [
              if (imagePath != null)
                Positioned.fill(
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(24),
                    child: Image.file(File(imagePath!), fit: BoxFit.cover),
                  ),
                )
              else
                const Icon(Icons.eco_rounded, size: 220, color: AppColors.leaf),
              Positioned(
                bottom: 16,
                child: Text(
                  'DEMO LEAF IMAGE',
                  style: AppFonts.caption.copyWith(
                    color: AppColors.surface,
                    letterSpacing: 1.5,
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 14),
        Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: AppColors.successSurface,
            borderRadius: BorderRadius.circular(16),
          ),
          child: Row(
            children: [
              const Icon(Icons.check_circle_outline, color: AppColors.tertiary),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  'Image looks good! Make sure the leaf is clear and well lit.',
                  style: AppFonts.caption.copyWith(
                    color: AppColors.primary,
                    height: 1.4,
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 20),
        Row(
          children: [
            Expanded(
              child: SecondaryButton(label: 'Retake', onPressed: onRetake),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: PrimaryButton(label: 'Continue', onPressed: onContinue),
            ),
          ],
        ),
      ],
    ),
  );
}
