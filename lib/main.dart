import 'dart:developer' as developer;

import 'package:cropdoc/router/router.dart';
import 'package:cropdoc/services/auth_service.dart';
import 'package:cropdoc/themes/app_colors.dart';
import 'package:cropdoc/themes/app_fonts.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  try {
    await AuthSession.restore();
  } catch (error, stackTrace) {
    developer.log(
      'Could not restore the saved login session.',
      error: error,
      stackTrace: stackTrace,
      name: 'cropdoc.auth',
      level: 1000,
    );
  }
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = ThemeData(
      fontFamily: AppFonts.bodyFontFamily,
      useMaterial3: true,
      colorScheme: const ColorScheme.light(
        primary: AppColors.primary,
        onPrimary: AppColors.surface,
        secondary: AppColors.secondary,
        onSecondary: AppColors.primary,
        tertiary: AppColors.tertiary,
        onTertiary: AppColors.primary,
        error: AppColors.error,
        onError: AppColors.surface,
        surface: AppColors.surface,
        onSurface: AppColors.primary,
      ),
      scaffoldBackgroundColor: AppColors.background,
      appBarTheme: const AppBarTheme(
        backgroundColor: AppColors.background,
        foregroundColor: AppColors.primary,
        elevation: 0,
        scrolledUnderElevation: 0,
      ),
    );

    return ProviderScope(
      child: MaterialApp.router(
        routerConfig: appRouter,
        title: "CropDoc",
        debugShowCheckedModeBanner: false,
        themeMode: ThemeMode.light,
        theme: theme.copyWith(
          textTheme: theme.textTheme.copyWith(
            displayLarge: _headingStyle(theme.textTheme.displayLarge),
            displayMedium: _headingStyle(theme.textTheme.displayMedium),
            displaySmall: _headingStyle(theme.textTheme.displaySmall),
            headlineLarge: _headingStyle(theme.textTheme.headlineLarge),
            headlineMedium: _headingStyle(theme.textTheme.headlineMedium),
            headlineSmall: _headingStyle(theme.textTheme.headlineSmall),
            titleLarge: _headingStyle(theme.textTheme.titleLarge),
            titleMedium: _headingStyle(theme.textTheme.titleMedium),
            titleSmall: _headingStyle(theme.textTheme.titleSmall),
          ),
        ),
      ),
    );
  }
}

TextStyle? _headingStyle(TextStyle? style) =>
    style?.copyWith(fontFamily: AppFonts.headingFontFamily);
