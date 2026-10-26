import 'dart:async';

import 'package:cropdoc/screens/scan_stages/results_stage.dart';
import 'package:cropdoc/services/auth_service.dart';
import 'package:cropdoc/themes/app_colors.dart';
import 'package:cropdoc/themes/app_fonts.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class History extends ConsumerStatefulWidget {
  final VoidCallback onBack;

  const History({super.key, required this.onBack});

  @override
  ConsumerState<History> createState() => _HistoryState();
}

class _HistoryState extends ConsumerState<History> {
  List<_ScanRecord> _scans = [];
  _HistoryFilter _selectedFilter = _HistoryFilter.all;
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    unawaited(_loadHistory());
  }

  Future<void> _loadHistory() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final response = await ref.read(apiClientProvider).getPredictions();
      final content = response is Map ? response['content'] : null;
      final records = content is List
          ? content
                .whereType<Map>()
                .map(_ScanRecord.fromJson)
                .toList(growable: false)
          : const <_ScanRecord>[];
      if (!mounted) return;
      setState(() {
        _scans = records;
        _isLoading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _error = 'Could not load scan history. Please try again.';
        _isLoading = false;
      });
    }
  }

  List<_ScanRecord> get _visibleScans => _scans.where((scan) {
    return switch (_selectedFilter) {
      _HistoryFilter.all => true,
      _HistoryFilter.unhealthy => scan.status == _ScanStatus.unhealthy,
      _HistoryFilter.healthy => scan.status == _ScanStatus.healthy,
    };
  }).toList();

  @override
  Widget build(BuildContext context) {
    final scans = _visibleScans;

    return Container(
      color: AppColors.background,
      child: Column(
        children: [
          SizedBox(
            height: 54,
            child: Stack(
              alignment: Alignment.center,
              children: [
                Text(
                  'History',
                  style: AppFonts.screenTitle.copyWith(
                    color: AppColors.primary,
                    fontSize: 19,
                  ),
                ),
                Align(
                  alignment: Alignment.centerLeft,
                  child: IconButton(
                    tooltip: 'Back to home',
                    onPressed: widget.onBack,
                    icon: const Icon(Icons.arrow_back_rounded),
                    color: AppColors.primary,
                  ),
                ),
                Align(
                  alignment: Alignment.centerRight,
                  child: IconButton(
                    tooltip: 'Refresh history',
                    onPressed: _isLoading ? null : _loadHistory,
                    icon: const Icon(Icons.refresh_rounded),
                    color: AppColors.primary,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 8),
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 0, 12, 10),
            child: Row(
              children: [
                _FilterTab(
                  label: 'All',
                  selected: _selectedFilter == _HistoryFilter.all,
                  onTap: () =>
                      setState(() => _selectedFilter = _HistoryFilter.all),
                ),
                _FilterTab(
                  label: 'Unhealthy',
                  selected: _selectedFilter == _HistoryFilter.unhealthy,
                  onTap: () => setState(
                    () => _selectedFilter = _HistoryFilter.unhealthy,
                  ),
                ),
                _FilterTab(
                  label: 'Healthy',
                  selected: _selectedFilter == _HistoryFilter.healthy,
                  onTap: () =>
                      setState(() => _selectedFilter = _HistoryFilter.healthy),
                ),
              ],
            ),
          ),
          Expanded(
            child: _isLoading
                ? const Center(
                    child: CircularProgressIndicator(color: AppColors.primary),
                  )
                : _error != null
                ? Center(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(
                          _error!,
                          textAlign: TextAlign.center,
                          style: AppFonts.caption.copyWith(
                            color: AppColors.error,
                            fontSize: 14,
                          ),
                        ),
                        TextButton(
                          onPressed: _loadHistory,
                          child: const Text('Retry'),
                        ),
                      ],
                    ),
                  )
                : scans.isEmpty
                ? Center(
                    child: Text(
                      'No scans in this category yet.',
                      style: AppFonts.caption.copyWith(
                        color: AppColors.muted,
                        fontSize: 14,
                      ),
                    ),
                  )
                : ListView.separated(
                    padding: const EdgeInsets.fromLTRB(0, 0, 0, 94),
                    itemCount: scans.length,
                    separatorBuilder: (context, index) =>
                        const SizedBox(height: 4),
                    itemBuilder: (context, index) => _HistoryCard(
                      record: scans[index],
                      onTap: () => _showReport(scans[index]),
                    ),
                  ),
          ),
        ],
      ),
    );
  }

  Future<void> _showReport(_ScanRecord record) =>
      Navigator.of(context).push<void>(
        MaterialPageRoute<void>(
          builder: (context) => Scaffold(
            backgroundColor: AppColors.background,
            appBar: AppBar(
              backgroundColor: AppColors.background,
              foregroundColor: AppColors.primary,
              title: Text(
                'Scan report',
                style: AppFonts.button.copyWith(
                  color: AppColors.primary,
                  fontSize: 16,
                ),
              ),
              centerTitle: true,
            ),
            body: ResultsStage(
              imagePath: null,
              imageUrl: record.imageUrl,
              prediction: record.prediction,
            ),
          ),
        ),
      );
}

enum _HistoryFilter { all, unhealthy, healthy }

enum _ScanStatus { unhealthy, healthy }

class _ScanRecord {
  final String plantName;
  final _ScanStatus status;
  final DateTime? createdAt;
  final String? imageUrl;
  final Map<String, Object?> prediction;

  const _ScanRecord({
    required this.plantName,
    required this.status,
    required this.createdAt,
    required this.imageUrl,
    required this.prediction,
  });

  factory _ScanRecord.fromJson(Map<dynamic, dynamic> json) {
    final disease = json['predictedDisease'] is String
        ? json['predictedDisease'] as String
        : 'Unknown result';
    final crop = json['crop'] is String ? json['crop'] as String : 'Plant';
    final statusValue = json['status'] is String
        ? (json['status'] as String).toLowerCase()
        : '';
    final healthy =
        disease.toLowerCase().contains('healthy') || statusValue == 'healthy';
    final createdAtValue = json['createdAt'];
    return _ScanRecord(
      plantName: '$crop - $disease',
      status: healthy ? _ScanStatus.healthy : _ScanStatus.unhealthy,
      createdAt: createdAtValue is String
          ? DateTime.tryParse(createdAtValue)
          : null,
      imageUrl: _readImageUrl(json),
      prediction: json.map((key, value) => MapEntry(key.toString(), value)),
    );
  }

  static String? _readImageUrl(Map<dynamic, dynamic> json) {
    for (final key in ['imageUrl', 'imageURL', 'image_url', 'image']) {
      final value = json[key];
      if (value is! String) continue;
      final uri = Uri.tryParse(value);
      if (uri != null &&
          (uri.scheme == 'https' || uri.scheme == 'http') &&
          uri.host.isNotEmpty) {
        return uri.toString();
      }
    }
    return null;
  }
}

class _FilterTab extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;

  const _FilterTab({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) => Expanded(
    child: Padding(
      padding: const EdgeInsets.symmetric(horizontal: 4),
      child: Semantics(
        button: true,
        selected: selected,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(12),
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 180),
            alignment: Alignment.center,
            height: 34,
            decoration: BoxDecoration(
              color: selected ? const Color(0xFF98C978) : Colors.transparent,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Text(
              label,
              style: AppFonts.caption.copyWith(
                color: AppColors.primary,
                fontSize: 12,
                fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
              ),
            ),
          ),
        ),
      ),
    ),
  );
}

class _HistoryCard extends StatelessWidget {
  final _ScanRecord record;
  final VoidCallback onTap;

  const _HistoryCard({required this.record, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final isHealthy = record.status == _ScanStatus.healthy;
    final tileColor = isHealthy
        ? const Color(0xFFEFF4D7)
        : const Color(0xFFFFE5D4);
    final badgeColor = isHealthy
        ? const Color(0xFF98C978)
        : const Color(0xFFFFCFC2);
    final badgeTextColor = isHealthy
        ? AppColors.primary
        : const Color(0xFFC52216);

    return Material(
      color: tileColor,
      borderRadius: BorderRadius.circular(9),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(9),
        child: SizedBox(
          height: 84,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
            child: Row(
              children: [
                Container(
                  width: 66,
                  height: 60,
                  decoration: BoxDecoration(
                    color: AppColors.primary,
                    borderRadius: BorderRadius.circular(9),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withValues(alpha: 0.2),
                        blurRadius: 5,
                        offset: const Offset(0, 2),
                      ),
                    ],
                  ),
                  clipBehavior: Clip.antiAlias,
                  child: record.imageUrl == null
                      ? const Icon(
                          Icons.eco_rounded,
                          color: AppColors.leaf,
                          size: 36,
                        )
                      : Image.network(
                          record.imageUrl!,
                          fit: BoxFit.cover,
                          errorBuilder: (context, error, stackTrace) =>
                              const Icon(
                                Icons.eco_rounded,
                                color: AppColors.leaf,
                                size: 36,
                              ),
                        ),
                ),
                const SizedBox(width: 9),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Container(
                        constraints: const BoxConstraints(minWidth: 118),
                        padding: const EdgeInsets.symmetric(
                          horizontal: 12,
                          vertical: 4,
                        ),
                        decoration: BoxDecoration(
                          color: badgeColor,
                          borderRadius: BorderRadius.circular(7),
                        ),
                        child: Text(
                          isHealthy
                              ? 'No Disease Detected'
                              : 'Disease Detected',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: AppFonts.caption.copyWith(
                            color: badgeTextColor,
                            fontSize: 12,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ),
                      const SizedBox(height: 3),
                      Text(
                        record.plantName,
                        style: AppFonts.caption.copyWith(
                          color: AppColors.primary,
                          fontSize: 12,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      Text(
                        record.createdAt == null
                            ? 'Date unavailable'
                            : '${record.createdAt!.toLocal().day}/'
                                  '${record.createdAt!.toLocal().month}/'
                                  '${record.createdAt!.toLocal().year}',
                        style: AppFonts.caption.copyWith(
                          color: AppColors.primary,
                          fontSize: 10,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
