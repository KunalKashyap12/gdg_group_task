import 'package:cropdoc/themes/app_colors.dart';
import 'package:cropdoc/themes/app_fonts.dart';
import 'package:flutter/material.dart';

class AnalyzingStage extends StatelessWidget {
  const AnalyzingStage({super.key});

  @override
  Widget build(BuildContext context) => Center(
    child: Padding(
      padding: const EdgeInsets.all(32),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          SizedBox(
            height: 150,
            width: 150,
            child: Stack(
              alignment: Alignment.center,
              children: [
                const CircularProgressIndicator(
                  strokeWidth: 10,
                  color: AppColors.tertiary,
                ),
                Container(
                  padding: const EdgeInsets.all(18),
                  decoration: const BoxDecoration(
                    color: AppColors.surface,
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    Icons.eco_rounded,
                    color: AppColors.tertiary,
                    size: 48,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 28),
          Text(
            'Analyzing your image...',
            style: AppFonts.screenTitle.copyWith(fontSize: 22),
          ),
          const SizedBox(height: 10),
          Text(
            'Our crop guide is checking the leaf for possible diseases.',
            textAlign: TextAlign.center,
            style: AppFonts.caption.copyWith(
              color: AppColors.muted,
              height: 1.5,
            ),
          ),
          const SizedBox(height: 22),
          Text(
            'This may take a few seconds.',
            style: AppFonts.caption.copyWith(color: AppColors.muted),
          ),
        ],
      ),
    ),
  );
}
