import 'package:cropdoc/router/routes.dart';
import 'package:cropdoc/services/auth_service.dart';
import 'package:cropdoc/screens/home.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:cropdoc/screens/history.dart';
import 'package:cropdoc/screens/profile.dart';
import 'package:cropdoc/screens/scan.dart';
import 'package:cropdoc/themes/app_colors.dart';
import 'package:cropdoc/themes/app_fonts.dart';
import 'package:go_router/go_router.dart';

class App extends ConsumerStatefulWidget {
  const App({super.key});

  @override
  ConsumerState<App> createState() => _AppState();
}

class _AppState extends ConsumerState<App> {
  int _currentIndex = 0;
  bool _showScan = false;

  @override
  Widget build(BuildContext context) {
    final pages = [
      Home(onStartScan: () => setState(() => _showScan = true)),
      History(
        onBack: () => setState(() {
          _showScan = false;
          _currentIndex = 0;
        }),
      ),
      Profile(
        onHistory: () => setState(() => _currentIndex = 2),
        onLogout: () async {
          final router = GoRouter.of(context);
          await ref.read(authServiceProvider).signOut();
          if (mounted) router.go(AppRoutes.signin);
        },
      ),
    ];
    final pageIndex = _currentIndex > 1 ? _currentIndex - 1 : 0;

    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle(
        statusBarColor: _showScan ? AppColors.primary : AppColors.background,
        statusBarIconBrightness: _showScan ? Brightness.light : Brightness.dark,
        statusBarBrightness: _showScan ? Brightness.dark : Brightness.light,
      ),
      child: Scaffold(
        body: Stack(
          children: [
            Positioned.fill(
              child: _showScan
                  ? ScanFlow(
                      onClose: () => setState(() {
                        _showScan = false;
                        _currentIndex = 0;
                      }),
                    )
                  : SafeArea(child: pages[pageIndex]),
            ),
            Positioned(
              left: 20,
              right: 20,
              bottom: 18,
              child: _FloatingBar(
                selectedIndex: _showScan ? 1 : _currentIndex,
                onSelected: (index) {
                  setState(() {
                    if (index == 1) {
                      _showScan = true;
                    } else {
                      _showScan = false;
                      _currentIndex = index;
                    }
                  });
                  HapticFeedback.mediumImpact();
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _FloatingBar extends StatelessWidget {
  final int selectedIndex;
  final ValueChanged<int> onSelected;
  const _FloatingBar({required this.selectedIndex, required this.onSelected});

  @override
  Widget build(BuildContext context) => Material(
    elevation: 8,
    shadowColor: AppColors.primary.withValues(alpha: .25),
    color: AppColors.surface,
    borderRadius: BorderRadius.circular(99),
    child: LayoutBuilder(
      builder: (context, constraints) => SizedBox(
        height: 55,
        child: Stack(
          children: [
            AnimatedPositioned(
              duration: const Duration(milliseconds: 420),
              curve: Curves.easeOutCubic,
              left: selectedIndex * constraints.maxWidth / 4,
              top: 0,
              bottom: 0,
              width: constraints.maxWidth / 4,
              child: DecoratedBox(
                decoration: BoxDecoration(
                  color: AppColors.primary,
                  borderRadius: BorderRadius.circular(99),
                ),
              ),
            ),
            Row(
              children: [
                Expanded(
                  child: _BarItem(
                    icon: Icons.home_outlined,
                    label: 'Home',
                    selected: selectedIndex == 0,
                    onTap: () => onSelected(0),
                  ),
                ),
                Expanded(
                  child: _BarItem(
                    icon: Icons.camera_alt_outlined,
                    label: 'Scan',
                    selected: selectedIndex == 1,
                    onTap: () => onSelected(1),
                  ),
                ),
                Expanded(
                  child: _BarItem(
                    icon: Icons.history_rounded,
                    label: 'History',
                    selected: selectedIndex == 2,
                    onTap: () => onSelected(2),
                  ),
                ),
                Expanded(
                  child: _BarItem(
                    icon: Icons.person_outline_rounded,
                    label: 'Profile',
                    selected: selectedIndex == 3,
                    onTap: () => onSelected(3),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    ),
  );
}

class _BarItem extends StatelessWidget {
  final IconData icon;
  final String label;
  final bool selected;
  final VoidCallback onTap;
  const _BarItem({
    required this.icon,
    required this.label,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) => Material(
    color: Colors.transparent,
    child: InkWell(
      onTap: onTap,
      splashColor: Colors.transparent,
      borderRadius: BorderRadius.circular(22),
      child: SizedBox(
        width: double.infinity,
        height: 52,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            AnimatedScale(
              scale: selected ? 1.08 : 1,
              duration: const Duration(milliseconds: 320),
              curve: Curves.easeOutBack,
              child: Icon(
                icon,
                size: 20,
                color: selected ? AppColors.leaf : AppColors.muted,
              ),
            ),
            const SizedBox(height: 3),
            AnimatedDefaultTextStyle(
              duration: const Duration(milliseconds: 280),
              curve: Curves.easeOutCubic,
              style: AppFonts.caption.copyWith(
                color: selected ? AppColors.surface : AppColors.muted,
                fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
              ),
              child: Text(label),
            ),
          ],
        ),
      ),
    ),
  );
}
