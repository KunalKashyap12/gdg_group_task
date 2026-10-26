import 'dart:async';
import 'dart:convert';
import 'dart:developer' as developer;

import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';

typedef JsonMap = Map<String, Object?>;

class ApiClient {
  static const defaultBaseUrl = 'https://crop-backend-r14q.onrender.com';
  static const _apiPath = 'api/v1/';
  static const _requestTimeout = Duration(seconds: 90);

  final Uri _baseUri;
  final Uri _apiRoot;
  final http.Client _httpClient;

  String? _accessToken;

  ApiClient({
    String baseUrl = defaultBaseUrl,
    http.Client? httpClient,
    this._accessToken,
  }) : _baseUri = _buildBaseUri(baseUrl),
       _apiRoot = _buildApiRoot(baseUrl),
       _httpClient = httpClient ?? http.Client();

  static Uri _buildBaseUri(String baseUrl) {
    final uri = Uri.tryParse(baseUrl);
    if (uri == null ||
        !uri.hasAuthority ||
        uri.host.isEmpty ||
        (uri.scheme != 'http' && uri.scheme != 'https')) {
      throw ArgumentError.value(
        baseUrl,
        'baseUrl',
        'Must be an absolute HTTP or HTTPS URL.',
      );
    }

    return uri.replace(
      path: uri.path.endsWith('/') ? uri.path : '${uri.path}/',
    );
  }

  static Uri _buildApiRoot(String baseUrl) =>
      _buildBaseUri(baseUrl).resolve(_apiPath);

  set accessToken(String? token) => _accessToken = token;

  void close() => _httpClient.close();

  Future<Object?> register(JsonMap payload) =>
      _sendJson('POST', 'auth/register', body: payload);

  Future<Object?> login(JsonMap payload) =>
      _sendJson('POST', 'auth/login', body: payload);

  Future<Object?> loginWithGoogle(JsonMap payload) =>
      _sendJson('POST', 'auth/google', body: payload);

  Future<Object?> getCurrentUser() => _sendJson('GET', 'users/me');

  Future<Object?> getPreferences() => _sendJson('GET', 'users/me/preferences');

  Future<Object?> updatePreferences(JsonMap payload) =>
      _sendJson('PUTP', 'users/me/preferences', body: payload);

  Future<Object?> getCrops() => _sendJson('GET', 'crops');

  Future<Object?> createPrediction({
    required String imagePath,
    String? crop,
  }) async {
    final extension = imagePath.split(RegExp(r'[/\\.]')).last.toLowerCase();
    final contentType = switch (extension) {
      'jpg' || 'jpeg' => MediaType('image', 'jpeg'),
      'png' => MediaType('image', 'png'),
      _ => throw ArgumentError.value(
        imagePath,
        'imagePath',
        'Only JPEG and PNG images are supported.',
      ),
    };
    final request = http.MultipartRequest(
      'POST',
      _baseUri.resolve('api/v1/predict'),
    );
    if (_accessToken case final token? when token.isNotEmpty) {
      request.headers['Authorization'] = 'Bearer $token';
    }
    if (crop != null && crop.trim().isNotEmpty) {
      request.fields['crop'] = crop.trim();
    }
    request.files.add(
      await http.MultipartFile.fromPath(
        'image',
        imagePath,
        contentType: contentType,
      ),
    );
    final response = await _sendRequest(request);
    return _decodeJsonResponse(response);
  }

  Future<Object?> getPredictions({int page = 0, int size = 20}) {
    if (page < 0) {
      throw ArgumentError.value(page, 'page', 'Must not be negative.');
    }
    if (size <= 0) {
      throw ArgumentError.value(size, 'size', 'Must be greater than zero.');
    }
    if (size > 50) {
      throw ArgumentError.value(size, 'size', 'Must not exceed 50.');
    }

    return _sendJson(
      'GET',
      'predictions',
      queryParameters: {'page': '$page', 'size': '$size'},
    );
  }

  Future<Object?> _sendJson(
    String method,
    String path, {
    Object? body,
    Map<String, String>? queryParameters,
  }) async {
    final response = await _send(
      method,
      path,
      body: body,
      queryParameters: queryParameters,
    );
    return _decodeJsonResponse(response);
  }

  Object? _decodeJsonResponse(http.Response response) {
    if (response.body.isEmpty) return null;

    try {
      return jsonDecode(response.body);
    } on FormatException catch (error) {
      throw ApiException(
        statusCode: response.statusCode,
        message: 'The server returned an invalid JSON response.',
        responseBody: response.body,
        cause: error,
      );
    }
  }

  Future<http.Response> _send(
    String method,
    String path, {
    Object? body,
    Map<String, String>? queryParameters,
  }) async {
    final uri = _apiRoot
        .resolve(path)
        .replace(queryParameters: queryParameters);
    final request = http.Request(method, uri)
      ..headers['Accept'] = 'application/json'
      ..headers['Content-Type'] = 'application/json';
    if (_accessToken case final token? when token.isNotEmpty) {
      request.headers['Authorization'] = 'Bearer $token';
    }
    if (body != null) request.body = jsonEncode(body);

    return _sendRequest(request);
  }

  Future<http.Response> _sendRequest(http.BaseRequest request) async {
    final stopwatch = Stopwatch()..start();
    final endpoint =
        '${request.method} ${request.url.path}'
        '${request.url.hasQuery ? '?${request.url.query}' : ''}';
    developer.log('API request started: $endpoint', name: 'cropdoc.api');

    try {
      final response = await _sendResponse(request).timeout(_requestTimeout);
      stopwatch.stop();
      if (response.statusCode < 200 || response.statusCode >= 300) {
        final message = _errorMessage(response.body);
        developer.log(
          'API request failed: $endpoint status=${response.statusCode} '
          'durationMs=${stopwatch.elapsedMilliseconds} message=$message\n'
          'response=${_logSafeResponse(response.body)}',
          name: 'cropdoc.api',
          level: 1000,
        );
        throw ApiException(
          statusCode: response.statusCode,
          message: message,
          responseBody: response.body,
        );
      }
      developer.log(
        'API request completed: $endpoint status=${response.statusCode} '
        'durationMs=${stopwatch.elapsedMilliseconds}\n'
        'response=${_logSafeResponse(response.body)}',
        name: 'cropdoc.api',
      );
      return response;
    } on ApiException {
      rethrow;
    } on TimeoutException catch (error, stackTrace) {
      stopwatch.stop();
      developer.log(
        'API request timed out: $endpoint '
        'durationMs=${stopwatch.elapsedMilliseconds}',
        name: 'cropdoc.api',
        error: error,
        stackTrace: stackTrace,
        level: 1000,
      );
      rethrow;
    } catch (error, stackTrace) {
      stopwatch.stop();
      developer.log(
        'API request failed: $endpoint '
        'durationMs=${stopwatch.elapsedMilliseconds}',
        name: 'cropdoc.api',
        error: error,
        stackTrace: stackTrace,
        level: 1000,
      );
      rethrow;
    }
  }

  Future<http.Response> _sendResponse(http.BaseRequest request) async {
    final streamedResponse = await _httpClient.send(request);
    return http.Response.fromStream(streamedResponse);
  }

  String _logSafeResponse(String body) {
    if (body.isEmpty) return '<empty>';

    Object? safeValue;
    try {
      safeValue = _redactSensitiveValues(jsonDecode(body));
    } on FormatException {
      safeValue = body;
    }

    final safeBody = safeValue is String ? safeValue : jsonEncode(safeValue);
    const maxLogLength = 4000;
    if (safeBody.length <= maxLogLength) return safeBody;
    return '${safeBody.substring(0, maxLogLength)}…<truncated>';
  }

  Object? _redactSensitiveValues(Object? value) {
    if (value is Map) {
      return value.map((key, value) {
        final normalizedKey = key.toString().toLowerCase();
        final isSensitive = const {
          'accesstoken',
          'access_token',
          'token',
          'jwt',
          'password',
          'credential',
          'idtoken',
          'authorization',
        }.contains(normalizedKey);
        return MapEntry(
          key,
          isSensitive ? '<redacted>' : _redactSensitiveValues(value),
        );
      });
    }
    if (value is List) {
      return value.map(_redactSensitiveValues).toList(growable: false);
    }
    return value;
  }

  String _errorMessage(String body) {
    if (body.isEmpty) return 'The server returned an error.';

    try {
      final decoded = jsonDecode(body);
      if (decoded is Map<String, dynamic>) {
        for (final key in ['message', 'detail', 'error']) {
          final message = decoded[key];
          if (message is String && message.isNotEmpty) return message;
        }
      }
    } on FormatException {
      return body;
    }

    return body;
  }
}

class ApiException implements Exception {
  final int statusCode;
  final String message;
  final String responseBody;
  final Object? cause;

  const ApiException({
    required this.statusCode,
    required this.message,
    required this.responseBody,
    this.cause,
  });

  @override
  String toString() => 'ApiException ($statusCode): $message';
}
