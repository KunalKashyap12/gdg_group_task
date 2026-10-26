import 'dart:io';

import 'package:cropdoc/themes/app_colors.dart';
import 'package:cropdoc/themes/app_fonts.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

class ResultsStage extends StatefulWidget {
  final String? imagePath;
  final String? imageUrl;
  final Map<String, Object?> prediction;

  const ResultsStage({
    super.key,
    required this.imagePath,
    this.imageUrl,
    required this.prediction,
  });

  @override
  State<ResultsStage> createState() => _ResultsStageState();
}

class _ResultsStageState extends State<ResultsStage> {
  bool _isSaved = false;

  @override
  Widget build(BuildContext context) {
    final disease = widget.prediction['predictedDisease'] is String
        ? widget.prediction['predictedDisease'] as String
        : 'Prediction unavailable';
    final crop = widget.prediction['crop'] is String
        ? widget.prediction['crop'] as String
        : 'Unknown crop';
    final status = widget.prediction['status'] is String
        ? widget.prediction['status'] as String
        : 'COMPLETED';
    final confidenceValue = widget.prediction['confidence'];
    final confidence = confidenceValue is num
        ? confidenceValue.toDouble().clamp(0.0, 1.0).toDouble()
        : 0.0;
    final guidanceValue = widget.prediction['guidance'];
    final guidance = guidanceValue is List
        ? guidanceValue.whereType<String>().toList()
        : const <String>[];
    final isHealthy = disease.toLowerCase().contains('healthy');
    final uncertain = status.toUpperCase() == 'UNCERTAIN';

    return Stack(
      children: [
        Positioned(
          right: -34,
          top: 280,
          child: IgnorePointer(
            child: Opacity(
              opacity: 0.95,
              child: SvgPicture.asset(
                'assets/svg/Leaf 8.svg',
                width: 130,
                height: 150,
              ),
            ),
          ),
        ),
        Positioned(
          left: -52,
          bottom: -12,
          child: IgnorePointer(
            child: SvgPicture.asset(
              'assets/svg/Leaf 7.svg',
              width: 150,
              height: 160,
            ),
          ),
        ),
        SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(14, 12, 14, 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              AspectRatio(
                aspectRatio: 1,
                child: Container(
                  decoration: BoxDecoration(
                    border: Border.all(color: AppColors.primary, width: 4),
                    borderRadius: BorderRadius.circular(22),
                  ),
                  clipBehavior: Clip.antiAlias,
                  child: widget.imagePath == null
                      ? widget.imageUrl == null
                            ? Image.asset(
                                'assets/images/home_leaf3.png',
                                fit: BoxFit.cover,
                              )
                            : Image.network(
                                widget.imageUrl!,
                                fit: BoxFit.cover,
                                errorBuilder: (context, error, stackTrace) =>
                                    Image.asset(
                                      'assets/images/home_leaf3.png',
                                      fit: BoxFit.cover,
                                    ),
                              )
                      : Image.file(
                          File(widget.imagePath!),
                          fit: BoxFit.cover,
                          errorBuilder: (context, error, stackTrace) =>
                              Image.asset(
                                'assets/images/home_leaf3.png',
                                fit: BoxFit.cover,
                              ),
                        ),
                ),
              ),
              const SizedBox(height: 36),
              Container(
                margin: const EdgeInsets.symmetric(horizontal: 4),
                padding: const EdgeInsets.fromLTRB(18, 18, 14, 16),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(20),
                  boxShadow: [
                    BoxShadow(
                      color: AppColors.primary.withValues(alpha: 0.2),
                      blurRadius: 5,
                      offset: const Offset(0, 3),
                    ),
                  ],
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 14,
                        vertical: 10,
                      ),
                      decoration: BoxDecoration(
                        color: isHealthy
                            ? AppColors.successSurface
                            : AppColors.errorSurface,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        uncertain
                            ? 'Uncertain Result'
                            : isHealthy
                            ? 'No Disease Detected'
                            : 'Disease Detected',
                        style: AppFonts.button.copyWith(
                          color: isHealthy
                              ? AppColors.tertiary
                              : AppColors.error,
                          fontSize: 14,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                    const SizedBox(height: 12),
                    Text(
                      '${(confidence * 100).round()}% Confidence',
                      style: AppFonts.button.copyWith(
                        color: AppColors.primary,
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 5),
                    ClipRRect(
                      borderRadius: BorderRadius.circular(20),
                      child: LinearProgressIndicator(
                        value: confidence,
                        minHeight: 8,
                        backgroundColor: Color(0xFFA4D792),
                        color: AppColors.primary,
                      ),
                    ),
                    const SizedBox(height: 12),
                    Text(
                      disease,
                      style: AppFonts.screenTitle.copyWith(
                        color: AppColors.primary,
                        fontSize: 24,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      'Crop: $crop\nStatus: $status',
                      style: AppFonts.caption.copyWith(
                        color: Colors.black,
                        fontSize: 15,
                        height: 1.65,
                      ),
                    ),
                    const SizedBox(height: 34),
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.fromLTRB(14, 16, 14, 16),
                      decoration: BoxDecoration(
                        color: AppColors.successSurface,
                        borderRadius: BorderRadius.circular(24),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              const Icon(
                                Icons.lightbulb_outline_rounded,
                                color: AppColors.primary,
                                size: 34,
                              ),
                              const SizedBox(width: 6),
                              Expanded(
                                child: Text(
                                  'Recommendations',
                                  style: AppFonts.screenTitle.copyWith(
                                    color: AppColors.primary,
                                    fontSize: 21,
                                  ),
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 12),
                          for (final recommendation in guidance)
                            _Recommendation(text: recommendation),
                          if (guidance.isEmpty)
                            const _Recommendation(
                              text: 'No recommendations were provided.',
                            ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),
              Align(
                alignment: Alignment.centerRight,
                child: SizedBox(
                  width: 178,
                  height: 48,
                  child: ElevatedButton(
                    onPressed: () {
                      setState(() => _isSaved = true);
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(
                          content: Text('Report saved for this session.'),
                        ),
                      );
                    },
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.primary,
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                    child: Text(
                      _isSaved ? 'Saved' : 'Save',
                      style: AppFonts.button.copyWith(fontSize: 22),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _Recommendation extends StatelessWidget {
  final String text;

  const _Recommendation({required this.text});

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(bottom: 10),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '•',
          style: TextStyle(
            color: AppColors.tertiary,
            fontSize: 18,
            height: 1.3,
          ),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            text,
            style: AppFonts.caption.copyWith(
              color: AppColors.tertiary,
              fontSize: 15,
              height: 1.5,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
      ],
    ),
  );
}
