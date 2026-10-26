import 'package:flutter/material.dart';
import 'package:cropdoc/themes/app_colors.dart';

class AppFonts {
  static const String bodyFontFamily = 'Poppins';
  static const String headingFontFamily = 'Roboto';
  static const String fontFamily = bodyFontFamily;

  static const TextStyle screenTitle = TextStyle(
    fontFamily: headingFontFamily,
    fontSize: 26.0,
    fontWeight: FontWeight.w700,
  );

  static const TextStyle caption = TextStyle(
    fontFamily: fontFamily,
    fontSize: 12.0,
    fontWeight: FontWeight.w400,
  );

  static const TextStyle button = TextStyle(
    fontFamily: fontFamily,
    fontSize: 14.0,
    fontWeight: FontWeight.w600,
    color: AppColors.surface,
  );
}
