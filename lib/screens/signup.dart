import 'dart:async';
import 'dart:developer' as developer;

import 'package:cropdoc/components/buttons.dart';
import 'package:cropdoc/components/text_field.dart';
import 'package:cropdoc/router/routes.dart';
import 'package:cropdoc/services/api_client.dart';
import 'package:cropdoc/services/auth_service.dart';
import 'package:cropdoc/themes/app_colors.dart';
import 'package:cropdoc/themes/app_fonts.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:go_router/go_router.dart';

class Signup extends ConsumerStatefulWidget {
  const Signup({super.key});

  @override
  ConsumerState<Signup> createState() => _SignupState();
}

class _SignupState extends ConsumerState<Signup> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();

  String? _error;
  bool _isLoading = false;

  @override
  void dispose() {
    _nameController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  Future<void> _handleSignUp() async {
    if (_isLoading || !_formKey.currentState!.validate()) return;
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      await ref
          .read(authServiceProvider)
          .register(
            fullName: _nameController.text,
            email: _emailController.text,
            password: _passwordController.text,
          );
      if (mounted) context.go(AppRoutes.home);
    } catch (error, stackTrace) {
      developer.log(
        'Failed to create the user account or save its profile.',
        error: error,
        stackTrace: stackTrace,
      );
      if (mounted) {
        setState(() {
          _error = switch (error) {
            ApiException(:final message) => message,
            AuthServiceException(:final message) => message,
            TimeoutException() =>
              'The server took too long to respond. Please try again.',
            _ => 'Could not create your account. Please try again.',
          };
        });
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _handleGoogleSignIn() async {
    if (_isLoading) return;
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      await ref.read(authServiceProvider).signInWithGoogle();
      if (mounted) context.go(AppRoutes.home);
    } on GoogleSignInException catch (error, stackTrace) {
      developer.log(
        'Google sign-in stopped during registration: code=${error.code} '
        'description=${error.description ?? '<none>'}',
        stackTrace: stackTrace,
        name: 'cropdoc.auth',
        level: 1000,
      );
      if (mounted) {
        setState(() {
          _error = error.code == GoogleSignInExceptionCode.canceled
              ? 'Google sign-in did not complete. Check the Android OAuth package name and SHA-1.'
              : 'Google sign-in failed: ${error.description ?? error.code.name}';
        });
      }
    } catch (error, stackTrace) {
      developer.log(
        'Google sign-in failed during registration.',
        error: error,
        stackTrace: stackTrace,
        name: 'cropdoc.auth',
        level: 1000,
      );
      if (mounted) {
        setState(() {
          _error = switch (error) {
            ApiException(:final message) => message,
            AuthServiceException(:final message) => message,
            TimeoutException() =>
              'The server took too long to respond. Please try again.',
            _ => 'Could not sign in with Google. Please try again.',
          };
        });
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      body: Stack(
        children: [
          Positioned(
            top: -34,
            right: -24,
            child: Transform.rotate(
              angle: -0.56,
              child: Opacity(
                opacity: 0.95,
                child: SvgPicture.asset(
                  'assets/svg/Leaf 8.svg',
                  width: 180,
                  height: 170,
                  colorFilter: const ColorFilter.mode(
                    Color(0xFF2B8A5A),
                    BlendMode.srcIn,
                  ),
                ),
              ),
            ),
          ),
          Positioned(
            bottom: -62,
            left: -42,
            child: Transform.rotate(
              angle: 2.5,
              child: Opacity(
                opacity: 0.95,
                child: SvgPicture.asset(
                  'assets/svg/Leaf 7.svg',
                  width: 210,
                  height: 190,
                  colorFilter: const ColorFilter.mode(
                    Color(0xFF2B8A5A),
                    BlendMode.srcIn,
                  ),
                ),
              ),
            ),
          ),
          SafeArea(
            child: Center(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(20),
                child: Container(
                  width: 360,
                  padding: const EdgeInsets.fromLTRB(20, 20, 20, 18),
                  decoration: BoxDecoration(
                    color: Colors.white70,
                    borderRadius: BorderRadius.circular(18),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withValues(alpha: 0.08),
                        blurRadius: 16,
                        offset: const Offset(0, 8),
                      ),
                    ],
                  ),
                  child: Form(
                    key: _formKey,
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Text(
                          'Register',
                          textAlign: TextAlign.center,
                          style: AppFonts.screenTitle.copyWith(
                            fontSize: 32,
                            color: AppColors.primary,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        const SizedBox(height: 18),
                        AppTextField(
                          controller: _nameController,
                          label: 'Name',
                          textCapitalization: TextCapitalization.words,
                          validator: (value) {
                            final name = (value ?? '').trim();
                            if (name.isEmpty) {
                              return 'Full name must not be blank.';
                            }
                            if (name.length < 2 || name.length > 100) {
                              return 'Name must be between 2 and 100 characters.';
                            }
                            return null;
                          },
                        ),
                        const SizedBox(height: 12),
                        AppTextField(
                          controller: _emailController,
                          label: 'Email',
                          keyboardType: TextInputType.emailAddress,
                          validator: (value) {
                            if ((value ?? '').trim().isEmpty) {
                              return 'Enter your email.';
                            }
                            return null;
                          },
                        ),
                        const SizedBox(height: 12),
                        AppTextField(
                          controller: _passwordController,
                          label: 'Password',
                          isPassword: true,
                          validator: (value) {
                            if ((value ?? '').length < 6) {
                              return 'Password must be at least 6 characters.';
                            }
                            return null;
                          },
                        ),
                        const SizedBox(height: 12),
                        AppTextField(
                          controller: _confirmPasswordController,
                          label: 'Confirm Password',
                          isPassword: true,
                          validator: (value) {
                            if (value != _passwordController.text) {
                              return 'Passwords do not match.';
                            }
                            return null;
                          },
                        ),
                        const SizedBox(height: 16),
                        if (_error != null) ...[
                          Text(
                            _error!,
                            textAlign: TextAlign.center,
                            style: const TextStyle(
                              color: AppColors.error,
                              fontSize: 12,
                            ),
                          ),
                          const SizedBox(height: 10),
                        ],
                        PrimaryButton(
                          label: 'Register',
                          onPressed: _handleSignUp,
                          isLoading: _isLoading,
                        ),
                        const SizedBox(height: 12),
                        GoogleSignInButton(
                          onPressed: _handleGoogleSignIn,
                          isLoading: _isLoading,
                          label: 'Sign In with Google',
                        ),
                        const SizedBox(height: 16),
                        Text(
                          'Already a user?',
                          textAlign: TextAlign.center,
                          style: AppFonts.caption.copyWith(
                            color: AppColors.primary,
                            fontWeight: FontWeight.w500,
                            fontSize: 13,
                          ),
                        ),
                        const SizedBox(height: 8),
                        SizedBox(
                          height: 48,
                          child: PrimaryButton(
                            isLoading: _isLoading,
                            onPressed: () => context.go(AppRoutes.signin),
                            label: 'Sign in',
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
