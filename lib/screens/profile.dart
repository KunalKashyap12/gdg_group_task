import 'dart:async';

import 'package:cropdoc/screens/about.dart';
import 'package:cropdoc/services/auth_service.dart';
import 'package:cropdoc/themes/app_colors.dart';
import 'package:cropdoc/themes/app_fonts.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class Profile extends ConsumerStatefulWidget {
  final VoidCallback onHistory;
  final VoidCallback onLogout;

  const Profile({super.key, required this.onHistory, required this.onLogout});

  @override
  ConsumerState<Profile> createState() => _ProfileState();
}

class _ProfileState extends ConsumerState<Profile> {
  bool _showAbout = false;
  String? _fullName;
  String? _profileError;

  @override
  void initState() {
    super.initState();
    unawaited(_loadProfile());
  }

  Future<void> _loadProfile() async {
    final api = ref.read(apiClientProvider);
    try {
      final response = await api.getCurrentUser();
      if (response is Map && response['fullName'] is String && mounted) {
        setState(() => _fullName = response['fullName'] as String);
      }
    } catch (_) {
      if (mounted) {
        setState(() => _profileError = 'Could not load your profile.');
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_showAbout) {
      return AboutScreen(onBack: () => setState(() => _showAbout = false));
    }

    return LayoutBuilder(
      builder: (context, constraints) => ColoredBox(
        color: AppColors.background,
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(14, 0, 14, 100),
          child: ConstrainedBox(
            constraints: BoxConstraints(minHeight: constraints.maxHeight - 100),
            child: Column(
              children: [
                const SizedBox(height: 20),
                Text(
                  'Profile',
                  style: AppFonts.screenTitle.copyWith(
                    color: AppColors.primary,
                    fontSize: 19,
                  ),
                ),
                const SizedBox(height: 26),
                Container(
                  width: 72,
                  height: 72,
                  decoration: const BoxDecoration(
                    color: Color(0xFFB7D9C7),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    Icons.person_outline_rounded,
                    color: AppColors.leaf,
                    size: 40,
                  ),
                ),
                const SizedBox(height: 10),
                Text(
                  _fullName ?? 'Profile',
                  style: AppFonts.screenTitle.copyWith(
                    color: AppColors.primary,
                    fontSize: 17,
                  ),
                ),
                if (_profileError != null) ...[
                  const SizedBox(height: 8),
                  Text(
                    _profileError!,
                    textAlign: TextAlign.center,
                    style: AppFonts.caption.copyWith(
                      color: AppColors.error,
                      fontSize: 12,
                    ),
                  ),
                ],
                const SizedBox(height: 74),
                Container(
                  decoration: BoxDecoration(
                    color: const Color(0xFFF0F4D6),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Column(
                    children: [
                      _ProfileItem(
                        icon: Icons.info_rounded,
                        label: 'About',
                        onTap: () => setState(() => _showAbout = true),
                      ),
                      _ProfileItem(
                        icon: Icons.history_rounded,
                        label: 'History',
                        onTap: widget.onHistory,
                      ),
                      _ProfileItem(
                        icon: Icons.info_rounded,
                        label: 'Terms & Services',
                        onTap: () => _showTerms(context),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 28),
                Material(
                  color: const Color(0xFFFFE5D4),
                  borderRadius: BorderRadius.circular(12),
                  child: InkWell(
                    borderRadius: BorderRadius.circular(12),
                    onTap: widget.onLogout,
                    child: const SizedBox(
                      width: double.infinity,
                      height: 42,
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(
                            Icons.logout_rounded,
                            color: AppColors.error,
                            size: 18,
                          ),
                          SizedBox(width: 9),
                          Text(
                            'Logout',
                            style: TextStyle(
                              color: AppColors.error,
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  void _showTerms(BuildContext context) {
    showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: AppColors.background,
        title: const Text(
          'Terms & Services',
          style: TextStyle(color: AppColors.primary),
        ),
        content: const Text(
          'Cropdoc provides crop-health information to support farmers. '
          'AI-assisted results are informational and should be considered '
          'alongside expert agricultural guidance.',
          style: TextStyle(color: AppColors.primary, height: 1.45),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text(
              'Close',
              style: TextStyle(color: AppColors.tertiary),
            ),
          ),
        ],
      ),
    );
  }
}

class _ProfileItem extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback? onTap;

  const _ProfileItem({required this.icon, required this.label, this.onTap});

  @override
  Widget build(BuildContext context) => Material(
    color: Colors.transparent,
    child: InkWell(
      onTap: onTap,
      child: SizedBox(
        height: 42,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14),
          child: Row(
            children: [
              Icon(icon, size: 16, color: AppColors.primary),
              const SizedBox(width: 9),
              Expanded(
                child: Text(
                  label,
                  style: AppFonts.caption.copyWith(
                    color: AppColors.primary,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    ),
  );
}
