import 'dart:async';

import 'package:flutter/services.dart';

/// Result code indicating that operation succeeded.
///
/// Same as [`Activity.RESULT_OK`](https://developer.android.com/reference/android/app/Activity#RESULT_OK)
/// but for the Dart world.
const kActivityResultOk = -1;

/// Result code indicating that operation canceled.
///
/// Same as [`Activity.RESULT_CANCELED`](https://developer.android.com/reference/android/app/Activity#RESULT_CANCELED)
/// but for the Dart world.
const kActivityResultCanceled = 0;

class Intent {
  static const FlagGrantReadUriPermission = 0x00000001;
  static const FlagGrantWriteUriPermission = 0x00000002;
  static const FlagFromBackground = 0x00000004;
  static const FlagDebugLogResolution = 0x00000008;
  static const FlagExcludeStoppedPackages = 0x00000010;
  static const FlagIncludeStoppedPackages = 0x00000020;
  static const FlagGrantPersistableUriPermission = 0x00000040;
  static const FlagGrantPrefixUriPermission = 0x00000080;
  
  final bool isNull;
  final String? componentClassName;
  final String? fromPackageName;
  final List<String>? fromSignatures;
  final String? action;
  final String? data;
  final int flags;
  final List<String>? categories;
  final Map<String, dynamic>? extra;

  bool get isNotNull => !isNull;

  const Intent({
    this.isNull = true,
    this.componentClassName,
    this.fromPackageName,
    this.fromSignatures,
    this.action,
    this.data,
    this.flags = 0,
    this.categories,
    this.extra,
  });

  factory Intent.fromMap(Map? map) => Intent(
        isNull: map == null,
        componentClassName: map?["componentClassName"],
        fromPackageName: map?["fromPackageName"],
        fromSignatures: map?["fromSignatures"] != null
            ? List.unmodifiable(
                (map!["fromSignatures"] as List).map((e) => e.toString()))
            : null,
        action: map?["action"],
        data: map?["data"],
        flags: (map?["flags"] as num?)?.toInt() ?? 0,
        categories: map?["categories"] != null
            ? List.unmodifiable(
                (map!["categories"] as List).map((e) => e.toString()))
            : null,
        extra: map?["extra"] != null
            ? (map!["extra"] as Map)
                .map((key, value) => MapEntry(key.toString(), value))
            : null,
      );

  Map<String, dynamic> toMap() => {
        "componentClassName": componentClassName,
        "fromPackageName": fromPackageName,
        "fromSignatures": fromSignatures,
        "action": action,
        "data": data,
        "flags": flags,
        "categories": categories,
        "extra": extra,
      };


  Intent addFlags(int flags) {
    return Intent(
      isNull: isNull,
      componentClassName: componentClassName,
      fromPackageName: fromPackageName,
      fromSignatures: fromSignatures,
      action: action,
      data: data,
      flags: this.flags | flags,
      categories: categories,
      extra: extra,
    );
  }

  Intent clearFlags(int flags) {
    return Intent(
      isNull: isNull,
      componentClassName: componentClassName,
      fromPackageName: fromPackageName,
      fromSignatures: fromSignatures,
      action: action,
      data: data,
      flags: this.flags & ~flags,
      categories: categories,
      extra: extra,
    );
  }

  bool hasFlags(int flags) {
    return (this.flags & flags) == flags;
  }
  
  @override
  String toString() {
    if (isNull) return 'Intent(null)';
    var str = 'Intent${toMap()}';
    return str.replaceFirst('{', '(').replaceFirst('}', ')', str.length - 1);
  }
}

class ReceiveIntent {
  static const MethodChannel _methodChannel = MethodChannel('receive_intent');
  static const EventChannel _eventChannel =
      EventChannel("receive_intent/event");

  static Future<Intent?> getInitialIntent() async {
    final renameMap = await _methodChannel.invokeMapMethod('getInitialIntent');
    //print("result: $renameMap");
    return Intent.fromMap(renameMap);
  }

  static Stream<Intent?> receivedIntentStream = _eventChannel
      .receiveBroadcastStream()
      .map<Intent?>((event) => Intent.fromMap(event as Map?));

  static Future<void> setResult(
    int resultCode, {
    Intent? intent,
    bool shouldFinish = false,
  }) {
    return _methodChannel.invokeMethod(
      'setResult',
      {
        'resultCode': resultCode,
        'intent': intent?.toMap(),
        'shouldFinish': shouldFinish,
      },
    );
  }
}
