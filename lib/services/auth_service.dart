import 'dart:developer' as developer;

import 'package:cropdoc/services/api_client.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_sign_in/google_sign_in.dart';

const _googleWebClientId =
    '358264495808-aig3vn0he5nu963oqgg83kr5sl32jcsp.apps.googleusercontent.com';

class AuthSession {
  static const _storage = FlutterSecureStorage();
  static const _tokenKey = 'cropdoc_access_token';
  static const _expiryKey = 'cropdoc_access_token_expiry';

  static String? accessToken;

  static bool get isAuthenticated => accessToken?.trim().isNotEmpty ?? false;

  static Future<void> restore() async {
    final token = await _storage.read(key: _tokenKey);
    final expiryValue = await _storage.read(key: _expiryKey);
    final expiryMillis = int.tryParse(expiryValue ?? '');
    if (token == null || token.trim().isEmpty || expiryMillis == null) {
      developer.log(
        'No complete saved login session was found.',
        name: 'cropdoc.auth',
      );
      await clear();
      return;
    }

    final expiry = DateTime.fromMillisecondsSinceEpoch(expiryMillis);
    if (!expiry.isAfter(DateTime.now())) {
      developer.log(
        'The saved login session has expired.',
        name: 'cropdoc.auth',
      );
      await clear();
      return;
    }
    accessToken = token;
    developer.log(
      'Restored saved login session; expires at ${expiry.toIso8601String()}.',
      name: 'cropdoc.auth',
    );
  }

  static Future<void> save(String token, DateTime expiry) async {
    await _storage.write(key: _tokenKey, value: token);
    await _storage.write(
      key: _expiryKey,
      value: '${expiry.millisecondsSinceEpoch}',
    );
    developer.log(
      'Saved login session; expires at ${expiry.toIso8601String()}.',
      name: 'cropdoc.auth',
    );
  }

  static Future<void> clear() async {
    accessToken = null;
    await _storage.delete(key: _tokenKey);
    await _storage.delete(key: _expiryKey);
  }
}

final apiClientProvider = Provider<ApiClient>((ref) {
  final client = ApiClient(accessToken: AuthSession.accessToken);
  ref.onDispose(client.close);
  return client;
});

final authServiceProvider = Provider<AuthService>(
  (ref) => AuthService(ref.watch(apiClientProvider)),
);

class AuthService {
  static Future<void>? _googleSignInInitialization;

  AuthService(this._apiClient);

  final ApiClient _apiClient;

  Future<void> signIn({required String email, required String password}) async {
    final response = await _apiClient.login({
      'email': email.trim(),
      'password': password,
    });
    await _setAccessToken(response);
  }

  Future<void> signInWithGoogle() async {
    developer.log('Google sign-in started.', name: 'cropdoc.auth');
    _googleSignInInitialization ??= GoogleSignIn.instance.initialize(
      serverClientId: _googleWebClientId,
    );
    await _googleSignInInitialization;
    developer.log(
      'Google Sign-In initialized; opening account chooser.',
      name: 'cropdoc.auth',
    );

    final account = await GoogleSignIn.instance.authenticate();
    developer.log(
      'Google account selected; checking for an ID token.',
      name: 'cropdoc.auth',
    );
    final idToken = account.authentication.idToken;
    if (idToken == null || idToken.isEmpty) {
      developer.log(
        'Google account did not return an ID token.',
        name: 'cropdoc.auth',
        level: 1000,
      );
      throw const AuthServiceException(
        'Google did not return an ID token. Check the OAuth client setup.',
      );
    }

    developer.log(
      'Google ID token received; exchanging it with the backend.',
      name: 'cropdoc.auth',
    );
    final response = await _apiClient.loginWithGoogle({'idToken': idToken});
    developer.log(
      'Google backend exchange succeeded; saving the app session.',
      name: 'cropdoc.auth',
    );
    await _setAccessToken(response);
    developer.log('Google sign-in completed.', name: 'cropdoc.auth');
  }

  Future<void> register({
    required String fullName,
    required String email,
    required String password,
  }) async {
    final normalizedFullName = fullName.trim();
    if (normalizedFullName.isEmpty) {
      throw const AuthServiceException('Full name must not be blank.');
    }

    final response = await _apiClient.register({
      'fullName': normalizedFullName,
      'email': email.trim(),
      'password': password,
    });
    await _setAccessToken(response);
  }

  Future<void> signOut() async {
    await AuthSession.clear();
    _apiClient.accessToken = null;
  }

  Future<void> _setAccessToken(Object? response) async {
    final tokenValue = response is Map ? response['accessToken'] : null;
    if (tokenValue is! String || tokenValue.trim().isEmpty) {
      throw const AuthServiceException(
        'The server response did not include an access token.',
      );
    }
    final expiryValue = response is Map ? response['expiresInSeconds'] : null;
    final expirySeconds = expiryValue is num
        ? expiryValue.toInt()
        : const Duration(hours: 1).inSeconds;
    await AuthSession.save(
      tokenValue.trim(),
      DateTime.now().add(Duration(seconds: expirySeconds)),
    );
    AuthSession.accessToken = tokenValue.trim();
    _apiClient.accessToken = tokenValue.trim();
  }
}

class AuthServiceException implements Exception {
  const AuthServiceException(this.message);

  final String message;

  @override
  String toString() => message;
}
