import 'package:cropdoc/router/routes.dart';
import 'package:cropdoc/screens/app.dart';
import 'package:cropdoc/screens/signin.dart';
import 'package:cropdoc/screens/splash.dart';
import 'package:cropdoc/screens/signup.dart';
import 'package:cropdoc/services/auth_service.dart';
import 'package:go_router/go_router.dart';

final GoRouter appRouter = GoRouter(
  initialLocation: AppRoutes.splash,
  redirect: (context, state) {
    final path = state.uri.path;
    final isAuthenticated = AuthSession.isAuthenticated;

    if (path == AppRoutes.app) {
      return isAuthenticated ? AppRoutes.home : AppRoutes.signin;
    }

    final isAuthRoute =
        path == AppRoutes.splash ||
        path == AppRoutes.signin ||
        path == AppRoutes.signup;
    if (!isAuthenticated && !isAuthRoute) return AppRoutes.signin;
    if (isAuthenticated && isAuthRoute) return AppRoutes.home;
    return null;
  },
  routes: [
    GoRoute(
      path: AppRoutes.splash,
      builder: (context, state) => const SplashScreen(),
    ),
    GoRoute(path: AppRoutes.app, builder: (context, state) => const SignIn()),
    GoRoute(path: AppRoutes.home, builder: (context, state) => const App()),
    GoRoute(
      path: AppRoutes.signup,
      builder: (context, state) => const Signup(),
    ),
    GoRoute(
      path: AppRoutes.signin,
      builder: (context, state) => const SignIn(),
    ),
  ],
);
