import 'dart:async';
import 'dart:developer' as developer;

import 'package:cropdoc/screens/suggestion_detail.dart';
import 'package:cropdoc/services/auth_service.dart';
import 'package:cropdoc/themes/app_colors.dart';
import 'package:cropdoc/themes/app_fonts.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_svg/flutter_svg.dart';

class Home extends ConsumerStatefulWidget {
  final VoidCallback? onStartScan;

  const Home({super.key, this.onStartScan});

  @override
  ConsumerState<Home> createState() => _HomeState();
}

class _HomeState extends ConsumerState<Home> {
  static const _suggestionImages = [
    'https://images.unsplash.com/photo-1501004318641-b39e6451bec6?auto=format&fit=crop&w=480&h=360&q=80',
    'https://images.unsplash.com/photo-1497250681960-ef046c08a56e?auto=format&fit=crop&w=480&h=360&q=80',
    'https://images.unsplash.com/photo-1466692476868-aef1dfb1e735?auto=format&fit=crop&w=480&h=360&q=80',
    'https://images.unsplash.com/photo-1523348837708-15d4a09cfac2?auto=format&fit=crop&w=480&h=360&q=80',
    'https://images.unsplash.com/photo-1518531933037-91b2f5f229cc?auto=format&fit=crop&w=480&h=360&q=80',
    'https://images.unsplash.com/photo-1500651230702-0e2d8a49d4ad?auto=format&fit=crop&w=480&h=360&q=80',
    'https://images.unsplash.com/photo-1485955900006-10f4d324d411?auto=format&fit=crop&w=480&h=360&q=80',
    'https://images.unsplash.com/photo-1530836369250-ef72a3f5cda8?auto=format&fit=crop&w=480&h=360&q=80',
  ];

  List<_CropInfo> _crops = [];
  bool _isLoadingCrops = true;
  String? _cropsError;
  String? _fullName;
  _SuggestionData? _selectedDetail;

  @override
  void initState() {
    super.initState();
    unawaited(_loadCrops());
    unawaited(_loadProfileName());
  }

  Future<void> _loadProfileName() async {
    try {
      final response = await ref.read(apiClientProvider).getCurrentUser();
      if (!mounted || response is! Map || response['fullName'] is! String) {
        return;
      }
      setState(() => _fullName = response['fullName'] as String);
    } catch (error, stackTrace) {
      developer.log(
        'Could not load the home greeting profile.',
        error: error,
        stackTrace: stackTrace,
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Could not load your profile name.')),
        );
      }
    }
  }

  Future<void> _loadCrops() async {
    setState(() {
      _isLoadingCrops = true;
      _cropsError = null;
    });
    try {
      final response = await ref.read(apiClientProvider).getCrops();
      if (response is! List) {
        throw const FormatException(
          'The crop catalog response was not a list.',
        );
      }
      final crops = response
          .whereType<Map>()
          .map(_CropInfo.fromJson)
          .where((crop) => crop.name.isNotEmpty)
          .toList();
      if (!mounted) return;
      setState(() {
        _crops = crops;
        _isLoadingCrops = false;
      });
    } catch (error, stackTrace) {
      developer.log(
        'Could not load Home suggestions from the crop catalog.',
        error: error,
        stackTrace: stackTrace,
      );
      if (!mounted) return;
      setState(() {
        _isLoadingCrops = false;
        _cropsError = 'Could not load suggestions from the backend.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final selectedDetail = _selectedDetail;
    if (selectedDetail != null) {
      return SuggestionDetailScreen(
        title: selectedDetail.title,
        imageUrl: selectedDetail.imageUrl,
        description: selectedDetail.description,
        sectionTitle: selectedDetail.sectionTitle,
        points: selectedDetail.points,
        onBack: () => setState(() => _selectedDetail = null),
      );
    }

    final diseaseItems = _diseaseItems;
    final tipItems = _tipItems;

    return SingleChildScrollView(
      padding: const EdgeInsets.only(bottom: 100),
      child: Stack(
        clipBehavior: Clip.none,
        children: [
          Positioned(
            top: 220,
            right: -58,
            child: IgnorePointer(
              child: Opacity(
                opacity: .88,
                child: SvgPicture.asset(
                  'assets/svg/Leaf 6.svg',
                  width: 190,
                  height: 230,
                  fit: BoxFit.contain,
                ),
              ),
            ),
          ),
          Positioned(
            top: 590,
            left: -58,
            child: IgnorePointer(
              child: Transform.rotate(
                angle: 3.14,
                child: SvgPicture.asset(
                  'assets/svg/Leaf 6.svg',
                  width: 150,
                  height: 190,
                  fit: BoxFit.contain,
                ),
              ),
            ),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
                child: SizedBox(
                  height: 112,
                  child: Row(
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                              'Hello, ${_fullName ?? 'Farmer'}',
                              style: AppFonts.caption.copyWith(
                                color: AppColors.primary,
                                fontSize: 14,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                            Text(
                              'Start detecting, for\na better growth.',
                              style: AppFonts.screenTitle.copyWith(
                                fontSize: 22,
                                height: 1.15,
                                color: AppColors.primary,
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 8),
                      SvgPicture.asset(
                        'assets/svg/home_flower.svg',
                        width: 64,
                        height: 76,
                        fit: BoxFit.contain,
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 10),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: Material(
                  color: AppColors.primary,
                  borderRadius: BorderRadius.circular(14),
                  clipBehavior: Clip.antiAlias,
                  child: InkWell(
                    onTap: widget.onStartScan,
                    child: SizedBox(
                      height: 146,
                      child: Stack(
                        children: [
                          Positioned(
                            right: 34,
                            bottom: -42,
                            child: Transform.rotate(
                              angle: -.45,
                              child: Opacity(
                                opacity: .52,
                                child: SvgPicture.asset(
                                  'assets/svg/Leaf 6.svg',
                                  width: 90,
                                  height: 115,
                                  fit: BoxFit.contain,
                                ),
                              ),
                            ),
                          ),
                          Padding(
                            padding: const EdgeInsets.fromLTRB(12, 12, 14, 12),
                            child: Row(
                              children: [
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    mainAxisAlignment: MainAxisAlignment.center,
                                    children: [
                                      Text(
                                        'SMARTER FARMING\nSTARTS HERE',
                                        style: AppFonts.button.copyWith(
                                          fontSize: 16,
                                          fontWeight: FontWeight.w700,
                                          height: 1.18,
                                          color: AppColors.surface,
                                        ),
                                      ),
                                      const SizedBox(height: 6),
                                      Text(
                                        'Your personal crop health assistant. Scan a leaf, detect potential diseases with AI.',
                                        maxLines: 3,
                                        overflow: TextOverflow.ellipsis,
                                        style: AppFonts.caption.copyWith(
                                          fontSize: 11,
                                          height: 1.2,
                                          color: AppColors.surface.withValues(
                                            alpha: .82,
                                          ),
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                                const SizedBox(width: 8),
                                SvgPicture.asset(
                                  'assets/svg/camera.svg',
                                  width: 78,
                                  height: 70,
                                  fit: BoxFit.contain,
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 18),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: Text(
                  'Suggestions',
                  style: AppFonts.screenTitle.copyWith(
                    fontSize: 20,
                    color: AppColors.primary,
                  ),
                ),
              ),
              const SizedBox(height: 16),
              if (_cropsError != null)
                Padding(
                  // padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
                  padding: EdgeInsets.zero,
                  child: Row(
                    children: [
                      Expanded(
                        child: Text(
                          _cropsError!,
                          style: AppFonts.caption.copyWith(
                            color: AppColors.error,
                            fontSize: 12,
                          ),
                        ),
                      ),
                      TextButton(
                        onPressed: _loadCrops,
                        child: const Text('Retry'),
                      ),
                    ],
                  ),
                ),
              _SuggestionSection(
                title: 'Common Diseases',
                items: diseaseItems,
                isLoading: _isLoadingCrops,
                onSelect: (item) => setState(() => _selectedDetail = item),
              ),
              const SizedBox(height: 14),
              _SuggestionSection(
                title: 'Gardening Tips',
                items: tipItems,
                isLoading: _isLoadingCrops,
                onSelect: (item) => setState(() => _selectedDetail = item),
              ),
            ],
          ),
        ],
      ),
    );
  }

  List<_SuggestionData> get _diseaseItems {
    var imageIndex = 0;
    return [
      for (final crop in _crops)
        for (final disease in crop.diseases)
          _SuggestionData(
            title: disease,
            imageUrl:
                crop.imageUrl ??
                _suggestionImages[imageIndex++ % _suggestionImages.length],
            description: crop.description.isEmpty ? null : crop.description,
            sectionTitle: 'Prevention for ${crop.name}',
            points: crop.preventiveTips,
          ),
    ];
  }

  List<_SuggestionData> get _tipItems {
    var imageIndex = 0;
    return [
      for (final crop in _crops)
        if (crop.preventiveTips.isNotEmpty)
          _SuggestionData(
            title: crop.name,
            imageUrl:
                crop.imageUrl ??
                _suggestionImages[(imageIndex++ + 4) %
                    _suggestionImages.length],
            description: crop.description.isEmpty ? null : crop.description,
            sectionTitle: 'Crop care',
            points: crop.preventiveTips,
          ),
    ];
  }
}

class _SuggestionSection extends StatelessWidget {
  final String title;
  final List<_SuggestionData> items;
  final bool isLoading;
  final ValueChanged<_SuggestionData> onSelect;

  const _SuggestionSection({
    required this.title,
    required this.items,
    required this.isLoading,
    required this.onSelect,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      // margin: const EdgeInsets.symmetric(horizontal: 16),
      padding: const EdgeInsets.fromLTRB(10, 12, 0, 12),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: .92),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.only(right: 12),
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    title,
                    style: AppFonts.button.copyWith(
                      color: AppColors.primary,
                      fontSize: 14,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 10),
          if (isLoading)
            const SizedBox(
              height: 108,
              child: Center(child: CircularProgressIndicator()),
            )
          else if (items.isEmpty)
            SizedBox(
              height: 108,
              child: Align(
                alignment: Alignment.centerLeft,
                child: Padding(
                  padding: const EdgeInsets.only(right: 12),
                  child: Text(
                    'No suggestions available from the backend.',
                    style: AppFonts.caption.copyWith(
                      color: AppColors.primary,
                      fontSize: 12,
                    ),
                  ),
                ),
              ),
            )
          else
            SizedBox(
              height: 294,
              child: GridView.builder(
                scrollDirection: Axis.horizontal,
                padding: EdgeInsets.zero,
                itemCount: items.length,
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 2,
                  mainAxisSpacing: 8,
                  crossAxisSpacing: 1,
                  mainAxisExtent: 95,
                ),
                itemBuilder: (context, index) => _SuggestionCard(
                  item: items[index],
                  onTap: () => onSelect(items[index]),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _SuggestionCard extends StatelessWidget {
  final _SuggestionData item;
  final VoidCallback onTap;

  const _SuggestionCard({required this.item, required this.onTap});

  @override
  Widget build(BuildContext context) => Material(
    color: Colors.transparent,
    borderRadius: BorderRadius.circular(8),
    child: InkWell(
      onTap: onTap,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(8),
            child: SizedBox(
              width: 95,
              height: 95,
              child: Image.network(
                item.imageUrl!,
                fit: BoxFit.cover,
                loadingBuilder: (context, child, progress) {
                  if (progress == null) return child;
                  return const ColoredBox(
                    color: AppColors.successSurface,
                    child: Center(
                      child: CircularProgressIndicator(strokeWidth: 2),
                    ),
                  );
                },
                errorBuilder: (context, error, stackTrace) => const ColoredBox(
                  color: AppColors.successSurface,
                  child: Center(
                    child: Icon(
                      Icons.eco_rounded,
                      color: AppColors.tertiary,
                      size: 32,
                    ),
                  ),
                ),
              ),
            ),
          ),
          SizedBox(
            height: 32,
            child: Center(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 4),
                child: Text(
                  item.title,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  textAlign: TextAlign.center,
                  style: AppFonts.caption.copyWith(
                    color: AppColors.primary,
                    fontSize: 10,
                    fontWeight: FontWeight.w600,
                    height: 1.1,
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    ),
  );
}

class _SuggestionData {
  final String title;
  final String? imageUrl;
  final String? description;
  final String sectionTitle;
  final List<String> points;

  const _SuggestionData({
    required this.title,
    this.imageUrl,
    required this.description,
    required this.sectionTitle,
    required this.points,
  });
}

class _CropInfo {
  final String name;
  final String? imageUrl;
  final String description;
  final List<String> diseases;
  final List<String> preventiveTips;

  const _CropInfo({
    required this.name,
    required this.imageUrl,
    required this.description,
    required this.diseases,
    required this.preventiveTips,
  });

  factory _CropInfo.fromJson(Map<dynamic, dynamic> json) => _CropInfo(
    name: json['name'] is String ? (json['name'] as String).trim() : '',
    imageUrl: _readImageUrl(json),
    description: json['description'] is String
        ? json['description'] as String
        : '',
    diseases: json['supportedDiseases'] is List
        ? (json['supportedDiseases'] as List)
              .whereType<String>()
              .map((value) => value.trim())
              .where((value) => value.isNotEmpty)
              .toList()
        : const [],
    preventiveTips: json['preventiveTips'] is List
        ? (json['preventiveTips'] as List)
              .whereType<String>()
              .map((value) => value.trim())
              .where((value) => value.isNotEmpty)
              .toList()
        : const [],
  );

  static String? _readImageUrl(Map<dynamic, dynamic> json) {
    for (final key in ['imageUrl', 'imageURL', 'image_url', 'image']) {
      final value = json[key];
      if (value is String && Uri.tryParse(value)?.hasAbsolutePath == true) {
        final uri = Uri.tryParse(value);
        if (uri != null &&
            (uri.scheme == 'https' || uri.scheme == 'http') &&
            uri.host.isNotEmpty) {
          return uri.toString();
        }
      }
    }
    return null;
  }
}
