import { useEffect, useMemo, useState } from "react";
import adminApi from "../../apis/adminApi";

function expiryBadge(value) {
  if (!value) return null;
  const expiry = new Date(value);
  if (Number.isNaN(expiry.getTime())) return null;
  const diff = expiry.getTime() - Date.now();
  if (diff < 0) {
    return { label: "Đã hết hạn", className: "bg-red-100 text-red-700" };
  }
  if (diff <= 24 * 60 * 60 * 1000) {
    const hours = Math.max(1, Math.ceil(diff / (60 * 60 * 1000)));
    return { label: `Sắp hết hạn (${hours}h)`, className: "bg-yellow-100 text-yellow-800" };
  }
  return null;
}

function formatDate(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString("vi-VN");
}

export default function StockManagement() {
  const [tab, setTab] = useState("inventory");
  const [products, setProducts] = useState([]);
  const [lowStock, setLowStock] = useState([]);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState(null);
  const [newQuantity, setNewQuantity] = useState("");
  const [reason, setReason] = useState("");
  const [saving, setSaving] = useState(false);
  const [requestingId, setRequestingId] = useState("");

  const lowStockIds = useMemo(() => new Set(lowStock.map((item) => item.id)), [lowStock]);

  const loadInventory = async () => {
    const [pastryRes, lowRes] = await Promise.all([
      adminApi.getAdminPastries(),
      adminApi.getLowStockPastries(),
    ]);
    setProducts(pastryRes.data?.data || []);
    setLowStock(lowRes.data?.data || []);
  };

  const loadHistory = async () => {
    const res = await adminApi.getStockAdjustments();
    setHistory(res.data?.data || []);
  };

  useEffect(() => {
    Promise.all([loadInventory(), loadHistory()])
      .catch((err) => {
        console.error(err);
        alert("Không tải được dữ liệu tồn kho.");
      })
      .finally(() => setLoading(false));
  }, []);

  const requestBake = async (product) => {
    setRequestingId(product.id);
    try {
      await adminApi.requestProduction(product.id);
      alert("Đã gửi yêu cầu bếp nướng.");
    } catch (err) {
      console.error(err);
      alert("Yêu cầu này đã được gửi hoặc không gửi được.");
    } finally {
      setRequestingId("");
    }
  };

  const openAdjust = (product) => {
    setSelected(product);
    setNewQuantity(String(product.stockQuantity ?? 0));
    setReason("");
  };

  const handleAdjust = async (event) => {
    event.preventDefault();
    if (!reason.trim()) {
      alert("Nhập lý do điều chỉnh.");
      return;
    }
    const quantity = Number(newQuantity);
    if (!Number.isInteger(quantity) || quantity < 0) {
      alert("Số lượng mới phải là số nguyên không âm.");
      return;
    }
    setSaving(true);
    try {
      await adminApi.adjustStock({
        pastryId: selected.id,
        newQuantity: quantity,
        reason: reason.trim(),
      });
      setSelected(null);
      await Promise.all([loadInventory(), loadHistory()]);
    } catch (err) {
      console.error(err);
      alert("Không điều chỉnh được tồn kho.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-slate-800">Quản lý tồn kho</h2>
        <p className="text-sm text-slate-500">
          {lowStock.length} sản phẩm đang ở hoặc dưới ngưỡng tối thiểu.
        </p>
      </div>

      <div className="flex gap-2">
        <button
          type="button"
          onClick={() => setTab("inventory")}
          className={`rounded-lg px-4 py-2 text-sm ${tab === "inventory" ? "bg-slate-900 text-white" : "bg-white text-slate-700"}`}
        >
          Tồn kho
        </button>
        <button
          type="button"
          onClick={() => setTab("history")}
          className={`rounded-lg px-4 py-2 text-sm ${tab === "history" ? "bg-slate-900 text-white" : "bg-white text-slate-700"}`}
        >
          Nhật ký điều chỉnh
        </button>
      </div>

      {loading ? (
        <p className="text-sm text-slate-500">Đang tải...</p>
      ) : tab === "inventory" ? (
        <div className="overflow-x-auto rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-200">
              <tr>
                <th className="px-3 py-2">Sản phẩm</th>
                <th className="px-3 py-2">Tồn kho</th>
                <th className="px-3 py-2">Ngưỡng tối thiểu</th>
                <th className="px-3 py-2">Trạng thái</th>
                <th className="px-3 py-2">Hạn dùng</th>
                <th className="px-3 py-2"></th>
              </tr>
            </thead>
            <tbody>
              {products.map((product) => {
                const isLow =
                  lowStockIds.has(product.id) ||
                  Number(product.stockQuantity ?? 0) <= Number(product.minThreshold ?? 0);
                const expiry = expiryBadge(product.nearestExpiryDate);
                return (
                  <tr key={product.id} className="border-t border-slate-100">
                    <td className="px-3 py-3 font-medium">
                      {product.name}
                      {isLow && (
                        <span className="ml-2 rounded-full bg-red-100 px-2 py-0.5 text-xs font-semibold text-red-700">
                          Sắp hết
                        </span>
                      )}
                    </td>
                    <td className="px-3 py-3">{product.stockQuantity ?? 0}</td>
                    <td className="px-3 py-3">{product.minThreshold ?? 0}</td>
                    <td className="px-3 py-3">{product.status}</td>
                    <td className="px-3 py-3">
                      {expiry ? (
                        <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${expiry.className}`}>
                          {expiry.label}
                        </span>
                      ) : (
                        <span className="text-slate-400">{product.nearestExpiryDate ? formatDate(product.nearestExpiryDate) : "—"}</span>
                      )}
                    </td>
                    <td className="px-3 py-3 text-right">
                      <div className="flex justify-end gap-2">
                        {isLow && (
                          <button
                            type="button"
                            onClick={() => requestBake(product)}
                            disabled={requestingId === product.id}
                            className="rounded-lg border border-amber-700 px-3 py-1.5 text-xs text-amber-800 disabled:opacity-50"
                          >
                            Gửi yêu cầu Bếp nướng
                          </button>
                        )}
                        <button
                          type="button"
                          onClick={() => openAdjust(product)}
                          className="rounded-lg bg-amber-700 px-3 py-1.5 text-xs text-white"
                        >
                          Adjust Stock
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          {history.length === 0 ? (
            <p className="text-sm text-slate-500">Chưa có phiếu điều chỉnh.</p>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="border-b border-slate-200">
                <tr>
                  <th className="px-3 py-2">Thời gian</th>
                  <th className="px-3 py-2">Sản phẩm</th>
                  <th className="px-3 py-2">Cũ</th>
                  <th className="px-3 py-2">Mới</th>
                  <th className="px-3 py-2">Lý do</th>
                  <th className="px-3 py-2">Người điều chỉnh</th>
                </tr>
              </thead>
              <tbody>
                {history.map((row) => (
                  <tr key={row.id} className="border-t border-slate-100">
                    <td className="px-3 py-3">{formatDate(row.adjustedAt)}</td>
                    <td className="px-3 py-3 font-medium">{row.pastryName}</td>
                    <td className="px-3 py-3">{row.oldQuantity}</td>
                    <td className="px-3 py-3">{row.newQuantity}</td>
                    <td className="px-3 py-3">{row.reason}</td>
                    <td className="px-3 py-3">{row.adjustedByName || "N/A"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {selected && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <form onSubmit={handleAdjust} className="w-full max-w-md space-y-4 rounded-2xl bg-white p-6 shadow-xl">
            <h3 className="text-lg font-semibold text-slate-800">Adjust Stock</h3>
            <p className="text-sm text-slate-500">
              {selected.name} — tồn hiện tại {selected.stockQuantity ?? 0}
            </p>
            <label className="block text-sm">
              <span className="mb-1 block font-medium text-slate-700">Số lượng mới</span>
              <input
                type="number"
                min="0"
                step="1"
                value={newQuantity}
                onChange={(e) => setNewQuantity(e.target.value)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2"
              />
            </label>
            <label className="block text-sm">
              <span className="mb-1 block font-medium text-slate-700">Lý do</span>
              <textarea
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                required
                rows={3}
                className="w-full rounded-lg border border-slate-300 px-3 py-2"
                placeholder="Bắt buộc"
              />
            </label>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setSelected(null)}
                className="rounded-lg border border-slate-300 px-4 py-2 text-sm"
              >
                Hủy
              </button>
              <button
                type="submit"
                disabled={saving}
                className="rounded-lg bg-amber-700 px-4 py-2 text-sm text-white disabled:opacity-60"
              >
                {saving ? "Đang lưu..." : "Lưu điều chỉnh"}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
