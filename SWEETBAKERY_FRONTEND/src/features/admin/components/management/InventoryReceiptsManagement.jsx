import { useEffect, useState } from "react";
import adminApi from "../../apis/adminApi";

function formatPrice(v) {
  if (v == null) return "";
  return Number(v).toLocaleString("vi-VN") + "₫";
}

function formatDate(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString("vi-VN");
}

const emptyLine = () => ({ pastryId: "", quantity: 1, importPrice: 0 });

export default function InventoryReceiptsManagement() {
  const [products, setProducts] = useState([]);
  const [supplierName, setSupplierName] = useState("Xưởng sản xuất nội bộ");
  const [receiptCode, setReceiptCode] = useState("");
  const [lines, setLines] = useState([emptyLine()]);
  const [submitting, setSubmitting] = useState(false);
  const [receipts, setReceipts] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loadingHistory, setLoadingHistory] = useState(true);
  const [bakeRequests, setBakeRequests] = useState([]);
  const [lowStock, setLowStock] = useState([]);

  const loadProducts = async () => {
    const res = await adminApi.getAdminPastries();
    setProducts(res.data?.data || []);
  };

  const loadHistory = async (pageIndex = 0) => {
    setLoadingHistory(true);
    try {
      const res = await adminApi.getInventoryReceipts(pageIndex, 10);
      const pageData = res.data?.data || {};
      setReceipts(pageData.content || []);
      setTotalPages(pageData.totalPages || 0);
      setPage(pageData.number || 0);
    } catch (err) {
      console.error(err);
      alert("Không tải được lịch sử nhập kho.");
    } finally {
      setLoadingHistory(false);
    }
  };

  const loadBakeRequests = async () => {
    const res = await adminApi.getWarehouseNotifications();
    const requests = (res.data?.data || []).filter(
      (item) => item.type === "PRODUCTION_REQUEST" && !item.acknowledged
    );
    setBakeRequests(requests);
  };

  const loadLowStock = async () => {
    const res = await adminApi.getLowStockPastries();
    setLowStock(res.data?.data || []);
  };

  useEffect(() => {
    loadProducts().catch((err) => {
      console.error(err);
      alert("Không tải được danh sách bánh.");
    });
    loadHistory(0);
    loadBakeRequests().catch((err) => console.error(err));
    loadLowStock().catch((err) => console.error(err));
  }, []);

  const addPastryToForm = (pastryId) => {
    if (!pastryId) {
      return;
    }
    setLines((current) => {
      if (current.some((line) => line.pastryId === pastryId)) {
        return current;
      }
      const next = current[0]?.pastryId ? [...current] : [];
      next.push({ pastryId, quantity: 1, importPrice: 0 });
      return next.length > 0 ? next : [{ pastryId, quantity: 1, importPrice: 0 }];
    });
  };

  const acceptBakeRequest = async (request) => {
    addPastryToForm(request.pastryId);
    try {
      await adminApi.acknowledgeWarehouseNotification(request.id);
      setBakeRequests((current) => current.filter((item) => item.id !== request.id));
    } catch (err) {
      console.error(err);
    }
  };

  const updateLine = (index, field, value) => {
    setLines((current) => current.map((line, i) => (i === index ? { ...line, [field]: value } : line)));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const items = lines
      .filter((line) => line.pastryId)
      .map((line) => ({
        pastryId: line.pastryId,
        quantity: Number(line.quantity),
        importPrice: Number(line.importPrice),
      }));
    if (items.length === 0 || items.some((item) => item.quantity <= 0 || item.importPrice < 0)) {
      alert("Mỗi dòng cần chọn bánh, số lượng lớn hơn 0 và giá vốn không âm.");
      return;
    }

    setSubmitting(true);
    try {
      await adminApi.createInventoryReceipt({
        receiptCode: receiptCode.trim() || null,
        supplierName: supplierName.trim() || null,
        items,
      });
      setSupplierName("Xưởng sản xuất nội bộ");
      setReceiptCode("");
      setLines([emptyLine()]);
      await Promise.all([loadHistory(0), loadLowStock()]);
    } catch (err) {
      console.error(err);
      alert("Không tạo được phiếu nhập.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-2xl font-bold text-slate-800">Nhập bánh ra lò (Sản xuất nội bộ)</h2>
        <p className="text-sm text-slate-500">
          Ghi nhận số lượng bánh tươi vừa hoàn thành từ bếp đưa vào kho/tủ bán hàng.
        </p>
      </div>

      {lowStock.length > 0 && (
        <div className="rounded-2xl border border-rose-200 bg-rose-50 p-4">
          <p className="mb-2 text-sm font-semibold text-rose-800">
            Sản phẩm sắp hết hàng (Cần sản xuất thêm) — {lowStock.length}
          </p>
          <div className="flex flex-wrap gap-2">
            {lowStock.map((item) => (
              <button
                key={item.id}
                type="button"
                onClick={() => addPastryToForm(item.id)}
                className="rounded-full bg-white px-3 py-1 text-sm text-rose-800 shadow-sm"
              >
                {item.name} (Tồn: {item.stockQuantity}, Ngưỡng: {item.minThreshold})
              </button>
            ))}
          </div>
        </div>
      )}

      {bakeRequests.length > 0 && (
        <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4">
          <p className="mb-2 text-sm font-semibold text-amber-900">
            Yêu cầu bếp nướng ({bakeRequests.length})
          </p>
          <div className="flex flex-wrap gap-2">
            {bakeRequests.map((request) => (
              <button
                key={request.id}
                type="button"
                onClick={() => acceptBakeRequest(request)}
                className="rounded-full bg-white px-3 py-1 text-sm text-amber-900 shadow-sm"
              >
                {request.pastryName || request.message}
              </button>
            ))}
          </div>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <div className="grid gap-4 md:grid-cols-2">
          <label className="block text-sm">
            <span className="mb-1 block font-medium text-slate-700">Bộ phận / Ca sản xuất</span>
            <input
              value={supplierName}
              onChange={(e) => setSupplierName(e.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2"
              placeholder="Ví dụ: Bếp chính - Ca sáng"
            />
          </label>
          <label className="block text-sm">
            <span className="mb-1 block font-medium text-slate-700">Mã phiếu</span>
            <input
              value={receiptCode}
              onChange={(e) => setReceiptCode(e.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2"
              placeholder="Để trống để hệ thống tự tạo"
            />
          </label>
        </div>

        <div className="space-y-3">
          <div className="hidden gap-3 text-xs font-medium text-slate-500 md:grid md:grid-cols-[2fr_1fr_1fr_auto]">
            <span>Bánh</span>
            <span>Số lượng</span>
            <span>Giá vốn sản xuất (Đơn giá)</span>
            <span></span>
          </div>
          {lines.map((line, index) => (
            <div key={index} className="grid gap-3 md:grid-cols-[2fr_1fr_1fr_auto]">
              <select
                value={line.pastryId}
                onChange={(e) => updateLine(index, "pastryId", e.target.value)}
                className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
              >
                <option value="">Chọn bánh</option>
                {products.map((product) => (
                  <option key={product.id} value={product.id}>
                    {product.name} (tồn: {product.stockQuantity ?? 0})
                  </option>
                ))}
              </select>
              <input
                type="number"
                min="1"
                value={line.quantity}
                onChange={(e) => updateLine(index, "quantity", e.target.value)}
                className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
                placeholder="Số lượng"
              />
              <input
                type="number"
                min="0"
                value={line.importPrice}
                onChange={(e) => updateLine(index, "importPrice", e.target.value)}
                className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
                placeholder="Giá vốn sản xuất (Đơn giá)"
              />
              <button
                type="button"
                onClick={() => setLines((current) => current.filter((_, i) => i !== index))}
                disabled={lines.length === 1}
                className="rounded-lg px-3 text-sm text-red-600 disabled:opacity-40"
              >
                Xóa
              </button>
            </div>
          ))}
        </div>

        <div className="flex gap-3">
          <button
            type="button"
            onClick={() => setLines((current) => [...current, emptyLine()])}
            className="rounded-lg border border-slate-300 px-4 py-2 text-sm text-slate-700"
          >
            Thêm dòng
          </button>
          <button
            type="submit"
            disabled={submitting}
            className="rounded-lg bg-amber-700 px-4 py-2 text-sm text-white disabled:opacity-60"
          >
            {submitting ? "Đang lưu..." : "Lưu phiếu nhập"}
          </button>
        </div>
      </form>

      <div className="overflow-x-auto rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h3 className="mb-4 text-lg font-semibold text-slate-800">Lịch sử nhập kho</h3>
        {loadingHistory ? (
          <p className="text-sm text-slate-500">Đang tải...</p>
        ) : receipts.length === 0 ? (
          <p className="text-sm text-slate-500">Chưa có phiếu nhập.</p>
        ) : (
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-200">
              <tr>
                <th className="px-3 py-2">Mã phiếu</th>
                <th className="px-3 py-2">Bộ phận / Ca sản xuất</th>
                <th className="px-3 py-2">Ngày nhập</th>
                <th className="px-3 py-2">Người nhập</th>
                <th className="px-3 py-2">Tổng tiền</th>
                <th className="px-3 py-2">Chi tiết</th>
              </tr>
            </thead>
            <tbody>
              {receipts.map((receipt) => (
                <tr key={receipt.id} className="border-t border-slate-100 align-top">
                  <td className="px-3 py-3 font-medium">{receipt.receiptCode}</td>
                  <td className="px-3 py-3">{receipt.supplierName}</td>
                  <td className="px-3 py-3">{formatDate(receipt.importDate)}</td>
                  <td className="px-3 py-3">{receipt.createdByName || "N/A"}</td>
                  <td className="px-3 py-3 font-semibold text-amber-700">{formatPrice(receipt.totalAmount)}</td>
                  <td className="px-3 py-3 text-slate-600">
                    {(receipt.items || []).map((item) => (
                      <div key={item.id || `${item.pastryId}-${item.quantity}`}>
                        {item.pastryName} × {item.quantity} @ {formatPrice(item.importPrice)}
                      </div>
                    ))}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {totalPages > 1 && (
          <div className="mt-4 flex items-center justify-end gap-2">
            <button
              type="button"
              disabled={page <= 0}
              onClick={() => loadHistory(page - 1)}
              className="rounded-lg border border-slate-300 px-3 py-1 text-sm disabled:opacity-40"
            >
              Trước
            </button>
            <span className="text-sm text-slate-500">
              {page + 1} / {totalPages}
            </span>
            <button
              type="button"
              disabled={page + 1 >= totalPages}
              onClick={() => loadHistory(page + 1)}
              className="rounded-lg border border-slate-300 px-3 py-1 text-sm disabled:opacity-40"
            >
              Sau
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
