import 'dart:async';

import 'package:cropdoc/router/routes.dart';
import 'package:cropdoc/themes/app_colors.dart';
import 'package:cropdoc/themes/app_fonts.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:go_router/go_router.dart';

class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  bool _showLeaves = false;
  bool _showLogo = false;
  Timer? _exitTimer;
  Timer? _navigationTimer;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        setState(() {
          _showLeaves = true;
          _showLogo = true;
        });
      }
    });
    _exitTimer = Timer(const Duration(milliseconds: 2100), () {
      if (mounted) setState(() => _showLeaves = false);
    });
    _navigationTimer = Timer(const Duration(milliseconds: 3200), () {
      if (mounted) context.go(AppRoutes.home);
    });
  }

  @override
  void dispose() {
    _exitTimer?.cancel();
    _navigationTimer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => AnnotatedRegion<SystemUiOverlayStyle>(
    value: const SystemUiOverlayStyle(
      statusBarColor: AppColors.background,
      statusBarIconBrightness: Brightness.dark,
      statusBarBrightness: Brightness.light,
    ),
    child: Scaffold(
      backgroundColor: AppColors.background,
      body: LayoutBuilder(
        builder: (context, constraints) {
          final width = constraints.maxWidth;
          final height = constraints.maxHeight;
          return ClipRect(
            child: Stack(
              children: [
                AnimatedPositioned(
                  duration: const Duration(milliseconds: 850),
                  curve: Curves.easeInOutCubic,
                  top: _showLeaves ? -24 : -230,
                  right: _showLeaves ? -28 : -220,
                  child: SvgPicture.asset(
                    'assets/svg/Leaf 8.svg',
                    width: width * 0.52,
                    height: width * 0.52,
                  ),
                ),
                AnimatedPositioned(
                  duration: const Duration(milliseconds: 900),
                  curve: Curves.easeInOutCubic,
                  top: height * 0.13,
                  left: _showLeaves ? -18 : -170,
                  child: SvgPicture.asset(
                    'assets/svg/Leaf.svg',
                    width: width,
                    height: width,
                  ),
                ),
                AnimatedPositioned(
                  duration: const Duration(milliseconds: 950),
                  curve: Curves.easeInOutCubic,
                  bottom: _showLeaves ? -8 : -245,
                  left: _showLeaves ? -10 : -210,
                  child: SvgPicture.asset(
                    'assets/svg/Leaf 7.svg',
                    width: width * 0.42,
                    height: height * 0.25,
                  ),
                ),
                AnimatedPositioned(
                  duration: const Duration(milliseconds: 1000),
                  curve: Curves.easeInOutCubic,
                  bottom: _showLeaves ? -55 : -270,
                  right: _showLeaves ? 10 : -270,
                  child: Transform.rotate(
                    angle: 100,
                    child: Transform.flip(
                      flipX: true,
                      child: SvgPicture.asset(
                        'assets/svg/Leaf.svg',
                        width: width * 0.45,
                        height: height * 0.45,
                      ),
                    ),
                  ),
                ),
                Center(
                  child: AnimatedOpacity(
                    duration: const Duration(milliseconds: 500),
                    curve: Curves.easeOut,
                    opacity: _showLogo ? 1 : 0,
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        SvgPicture.asset(
                          'assets/svg/home_flower.svg',
                          width: 110,
                          height: 110,
                        ),
                        const SizedBox(height: 4),
                        const Text(
                          'CROPDOC',
                          style: TextStyle(
                            fontFamily: AppFonts.headingFontFamily,
                            color: AppColors.primary,
                            fontSize: 30,
                            fontWeight: FontWeight.w800,
                            letterSpacing: -0.5,
                            height: 1.1,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          );
        },
      ),
    ),
  );
}
