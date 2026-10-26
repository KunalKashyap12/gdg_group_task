import 'package:cropdoc/themes/app_colors.dart';
import 'package:cropdoc/themes/app_fonts.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

class AboutScreen extends StatelessWidget {
  final VoidCallback onBack;

  const AboutScreen({super.key, required this.onBack});

  @override
  Widget build(BuildContext context) => ColoredBox(
    color: AppColors.background,
    child: ClipRect(
      child: Stack(
        children: [
          Positioned(
            top: 24,
            right: -48,
            child: Transform.rotate(
              angle: -0.35,
              child: SvgPicture.asset(
                'assets/svg/Leaf 8.svg',
                width: 120,
                height: 120,
              ),
            ),
          ),
          Positioned(
            top: 195,
            left: -65,
            child: Transform.rotate(
              angle: 1.25,
              child: SvgPicture.asset(
                'assets/svg/Leaf 8.svg',
                width: 125,
                height: 125,
              ),
            ),
          ),
          Positioned(
            bottom: 112,
            right: -64,
            child: Transform.rotate(
              angle: 0.9,
              child: SvgPicture.asset(
                'assets/svg/Leaf 8.svg',
                width: 130,
                height: 130,
              ),
            ),
          ),
          Column(
            children: [
              SizedBox(
                height: 52,
                child: Stack(
                  alignment: Alignment.center,
                  children: [
                    Text(
                      'About',
                      style: AppFonts.screenTitle.copyWith(
                        color: AppColors.primary,
                        fontSize: 19,
                      ),
                    ),
                    Align(
                      alignment: Alignment.centerLeft,
                      child: IconButton(
                        tooltip: 'Back to profile',
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
                  padding: const EdgeInsets.fromLTRB(14, 10, 14, 110),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Center(
                        child: Text(
                          'Cropdoc',
                          style: AppFonts.screenTitle.copyWith(
                            color: AppColors.primary,
                            fontSize: 22,
                          ),
                        ),
                      ),
                      const SizedBox(height: 18),
                      const Text(
                        'We believe healthier crops begin with better '
                        'information. We use AI-powered crop disease detection '
                        'to help farmers identify potential plant health issues, '
                        'understand their crops, and make more informed farming '
                        'decisions. Our goal is to make crop care simpler, '
                        'smarter, and more accessible.',
                        style: TextStyle(
                          color: AppColors.primary,
                          fontSize: 13,
                          height: 1.42,
                        ),
                      ),
                      const SizedBox(height: 30),
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              'Our Mission',
                              style: AppFonts.screenTitle.copyWith(
                                color: AppColors.primary,
                                fontSize: 20,
                              ),
                            ),
                          ),
                          const Icon(
                            Icons.track_changes_rounded,
                            color: AppColors.primary,
                            size: 38,
                          ),
                        ],
                      ),
                      const SizedBox(height: 16),
                      const Text(
                        'Our mission is to make crop health monitoring simple '
                        'and accessible to every farmer. By turning leaf images '
                        'into useful insights, Cropdoc aims to help farmers '
                        'recognize potential diseases earlier, explore suitable '
                        'care guidance, and take informed steps toward '
                        'protecting their crops.',
                        style: TextStyle(
                          color: AppColors.primary,
                          fontSize: 13,
                          height: 1.42,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    ),
  );
}
