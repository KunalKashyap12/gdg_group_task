import 'package:cropdoc/themes/app_colors.dart';
import 'package:cropdoc/themes/app_fonts.dart';
import 'package:flutter/material.dart';

class SuggestionDetailScreen extends StatelessWidget {
  final String title;
  final String? imageUrl;
  final String? description;
  final String sectionTitle;
  final List<String> points;
  final VoidCallback onBack;

  const SuggestionDetailScreen({
    super.key,
    required this.title,
    this.imageUrl,
    required this.description,
    required this.sectionTitle,
    required this.points,
    required this.onBack,
  });

  @override
  Widget build(BuildContext context) => PopScope(
    canPop: false,
    onPopInvokedWithResult: (didPop, result) {
      if (!didPop) onBack();
    },
    child: ColoredBox(
      color: AppColors.background,
      child: Column(
        children: [
          SizedBox(
            height: 54,
            child: Stack(
              alignment: Alignment.center,
              children: [
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 54),
                  child: Text(
                    title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: AppFonts.screenTitle.copyWith(
                      color: AppColors.primary,
                      fontSize: 19,
                    ),
                  ),
                ),
                Align(
                  alignment: Alignment.centerLeft,
                  child: IconButton(
                    tooltip: 'Back',
                    onPressed: onBack,
                    icon: const Icon(Icons.arrow_back_rounded),
                    color: AppColors.primary,
                  ),
                ),
              ],
            ),
          ),
          Expanded(
            child: SingleChildScrollView(
              padding: const EdgeInsets.fromLTRB(12, 0, 12, 100),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  ClipRRect(
                    borderRadius: BorderRadius.circular(12),
                    child: AspectRatio(
                      aspectRatio: 1.12,
                      child: imageUrl == null
                          ? const ColoredBox(
                              color: AppColors.successSurface,
                              child: Icon(
                                Icons.eco_rounded,
                                color: AppColors.tertiary,
                                size: 72,
                              ),
                            )
                          : Image.network(
                              imageUrl!,
                              fit: BoxFit.cover,
                              errorBuilder: (context, error, stackTrace) =>
                                  const ColoredBox(
                                    color: AppColors.successSurface,
                                    child: Icon(
                                      Icons.eco_rounded,
                                      color: AppColors.tertiary,
                                      size: 72,
                                    ),
                                  ),
                            ),
                    ),
                  ),
                  if (description != null && description!.isNotEmpty) ...[
                    const SizedBox(height: 18),
                    Text(
                      'Description',
                      style: AppFonts.screenTitle.copyWith(
                        color: Colors.black,
                        fontSize: 19,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      description!,
                      style: AppFonts.caption.copyWith(
                        color: Colors.black87,
                        fontSize: 13,
                        height: 1.65,
                      ),
                    ),
                  ],
                  const SizedBox(height: 16),
                  Text(
                    sectionTitle,
                    style: AppFonts.screenTitle.copyWith(
                      color: AppColors.primary,
                      fontSize: 19,
                    ),
                  ),
                  const SizedBox(height: 8),
                  for (final point in points)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 6),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            '\u2022',
                            style: AppFonts.caption.copyWith(
                              color: AppColors.tertiary,
                              fontSize: 14,
                              height: 1.6,
                            ),
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              point,
                              style: AppFonts.caption.copyWith(
                                color: Colors.black87,
                                fontSize: 12,
                                height: 1.65,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                ],
              ),
            ),
          ),
        ],
      ),
    ),
  );
}
