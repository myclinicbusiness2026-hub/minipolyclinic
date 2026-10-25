import 'dart:convert';
import 'dart:typed_data';

/// ThermalPrintService
/// 
/// Generates ESC/POS thermal receipt commands for 58mm/80mm Bluetooth & USB POS Printers.
/// MANDATORY RULE: Used ONLY for Invoice Receipts.
class ThermalPrintService {
  // ESC/POS Command Constants
  static const int esc = 0x1B;
  static const int gs = 0x1D;
  static const int fs = 0x1C;

  // Initialize printer
  static final List<int> cmdInit = [esc, 0x40];

  // Alignment commands
  static final List<int> cmdAlignLeft = [esc, 0x61, 0];
  static final List<int> cmdAlignCenter = [esc, 0x61, 1];
  static final List<int> cmdAlignRight = [esc, 0x61, 2];

  // Font style commands
  static final List<int> cmdBoldOn = [esc, 0x45, 1];
  static final List<int> cmdBoldOff = [esc, 0x45, 0];
  static final List<int> cmdDoubleHeightOn = [esc, 0x21, 0x10];
  static final List<int> cmdDoubleWidthOn = [esc, 0x21, 0x20];
  static final List<int> cmdNormalText = [esc, 0x21, 0x00];

  // Paper cut command
  static final List<int> cmdCutPaper = [gs, 0x56, 0x42, 0x00];

  /// Formats and generates binary ESC/POS byte sequence for an invoice receipt
  static Uint8List generateEscPosReceipt({
    required String clinicName,
    required String clinicAddress,
    required String clinicPhone,
    required String invoiceNo,
    required String dateString,
    required String patientName,
    required String patientCode,
    required List<ThermalReceiptItem> items,
    required double subtotal,
    required double discount,
    required double grandTotal,
    required double paidAmount,
    required String paymentMethod,
    required String cashierName,
    bool is80mm = false,
  }) {
    final List<int> bytes = [];
    final int lineWidth = is80mm ? 48 : 32;

    // Helper: append text bytes
    void addText(String text) {
      bytes.addAll(utf8.encode(text));
    }

    void addLine(String text) {
      bytes.addAll(utf8.encode(text));
      bytes.add(0x0A); // LF
    }

    void addDivider(String char) {
      addLine(char * lineWidth);
    }

    // 1. Initialize
    bytes.addAll(cmdInit);

    // 2. Clinic Header (Center, Double-Height/Width)
    bytes.addAll(cmdAlignCenter);
    bytes.addAll(cmdDoubleHeightOn);
    bytes.addAll(cmdBoldOn);
    addLine(clinicName);
    bytes.addAll(cmdNormalText);
    addLine(clinicAddress);
    addLine('Tel: $clinicPhone');
    bytes.addAll(cmdBoldOff);

    addDivider('=');

    // 3. Invoice Metadata (Left Aligned)
    bytes.addAll(cmdAlignLeft);
    addLine('INVOICE RECEIPT');
    addLine('Invoice No : $invoiceNo');
    addLine('Date/Time  : $dateString');
    addLine('Patient HN : $patientCode');
    addLine('Patient    : $patientName');
    addLine('Cashier    : $cashierName');

    addDivider('-');

    // 4. Column Headers
    if (is80mm) {
      // 48 columns: Desc (26), Qty (6), Price (8), Total (8)
      final header = _padRight('ITEM DESCRIPTION', 26) +
          _padLeft('QTY', 6) +
          _padLeft('PRICE', 8) +
          _padLeft('TOTAL', 8);
      bytes.addAll(cmdBoldOn);
      addLine(header);
      bytes.addAll(cmdBoldOff);
    } else {
      // 32 columns: Desc (16), Qty (4), Total (12)
      final header = _padRight('ITEM', 16) +
          _padLeft('QTY', 4) +
          _padLeft('AMOUNT', 12);
      bytes.addAll(cmdBoldOn);
      addLine(header);
      bytes.addAll(cmdBoldOff);
    }

    addDivider('-');

    // 5. Line Items
    for (final item in items) {
      if (is80mm) {
        final desc = item.description.length > 25
            ? item.description.substring(0, 25)
            : item.description;
        final line = _padRight(desc, 26) +
            _padLeft('${item.quantity}', 6) +
            _padLeft('${item.unitPrice.toInt()}', 8) +
            _padLeft('${item.totalPrice.toInt()}', 8);
        addLine(line);
      } else {
        final desc = item.description.length > 15
            ? item.description.substring(0, 15)
            : item.description;
        final line = _padRight(desc, 16) +
            _padLeft('${item.quantity}', 4) +
            _padLeft('${item.totalPrice.toInt()} Ks', 12);
        addLine(line);
      }
    }

    addDivider('-');

    // 6. Summary Totals (Right Aligned or Column Formatted)
    String formatRow(String label, String value) {
      final spaces = lineWidth - label.length - value.length;
      return label + (' ' * (spaces > 0 ? spaces : 1)) + value;
    }

    addLine(formatRow('Subtotal:', '${subtotal.toInt()} Ks'));
    if (discount > 0) {
      addLine(formatRow('Discount:', '-${discount.toInt()} Ks'));
    }

    bytes.addAll(cmdBoldOn);
    bytes.addAll(cmdDoubleHeightOn);
    addLine(formatRow('GRAND TOTAL:', '${grandTotal.toInt()} Ks'));
    bytes.addAll(cmdNormalText);
    bytes.addAll(cmdBoldOff);

    addLine(formatRow('Payment Method:', paymentMethod));
    addLine(formatRow('Paid Amount:', '${paidAmount.toInt()} Ks'));

    final change = paidAmount - grandTotal;
    if (change > 0) {
      addLine(formatRow('Change Due:', '${change.toInt()} Ks'));
    }

    addDivider('=');

    // 7. Footer Greeting
    bytes.addAll(cmdAlignCenter);
    addLine('Thank you for choosing');
    addLine(clinicName);
    addLine('Wish you a speedy recovery!');
    addLine('*** ESC/POS Receipt Valid ***');

    // Feed lines & Cut paper
    bytes.addAll([0x0A, 0x0A, 0x0A]);
    bytes.addAll(cmdCutPaper);

    return Uint8List.fromList(bytes);
  }

  static String _padRight(String str, int length) {
    if (str.length >= length) return str.substring(0, length);
    return str + (' ' * (length - str.length));
  }

  static String _padLeft(String str, int length) {
    if (str.length >= length) return str.substring(0, length);
    return (' ' * (length - str.length)) + str;
  }
}

class ThermalReceiptItem {
  final String description;
  final int quantity;
  final double unitPrice;
  final double totalPrice;

  ThermalReceiptItem({
    required this.description,
    required this.quantity,
    required this.unitPrice,
    required this.totalPrice,
  });
}
