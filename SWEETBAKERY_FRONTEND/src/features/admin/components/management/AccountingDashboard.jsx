import { useEffect, useState } from "react";
import adminApi from "../../apis/adminApi";

function formatPrice(value) {
  return Number(value || 0).toLocaleString("vi-VN") + "₫";
}

function formatDate(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString("vi-VN");
}

const METHOD_LABELS = {
  CASH: "Tiền mặt",
  VIETQR: "VietQR / Chuyển khoản",
  OTHER: "Khác",
};

export default function AccountingDashboard() {
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    adminApi
      .getFinancialSummary()
      .then((res) => setSummary(res.data?.data || null))
      .catch((err) => {
        console.error(err);
        alert("Không tải được báo cáo kế toán.");
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return <p className="text-sm text-slate-500">Đang tải...</p>;
  }
  if (!summary) {
    return <p className="text-sm text-slate-500">Không có dữ liệu kế toán.</p>;
  }

  const breakdown = summary.paymentBreakdown || [];
  const maxAmount = Math.max(...breakdown.map((item) => Number(item.amount) || 0), 1);

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-slate-800">Kế toán & Doanh thu</h2>
        <p className="text-sm text-slate-500">Doanh thu đơn đã thanh toán trừ giá vốn sản xuất nội bộ.</p>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <p className="text-sm text-slate-500">Tổng doanh thu</p>
          <p className="mt-2 text-2xl font-bold text-emerald-700">{formatPrice(summary.totalRevenue)}</p>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <p className="text-sm text-slate-500">Giá vốn sản xuất</p>
          <p className="mt-2 text-2xl font-bold text-rose-700">{formatPrice(summary.totalImportCost)}</p>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <p className="text-sm text-slate-500">Lợi nhuận gộp</p>
          <p className="mt-2 text-2xl font-bold text-amber-700">{formatPrice(summary.grossProfit)}</p>
        </div>
      </div>

      <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h3 className="mb-4 text-lg font-semibold text-slate-800">Doanh thu theo phương thức thanh toán</h3>
        <div className="space-y-4">
          {breakdown.map((item) => (
            <div key={item.method}>
              <div className="mb-1 flex justify-between text-sm">
                <span className="font-medium text-slate-700">{METHOD_LABELS[item.method] || item.method}</span>
                <span className="text-slate-600">{formatPrice(item.amount)}</span>
              </div>
              <div className="h-2 rounded-full bg-slate-100">
                <div
                  className={`h-2 rounded-full ${item.method === "CASH" ? "bg-emerald-600" : "bg-amber-600"}`}
                  style={{ width: `${Math.max(4, ((Number(item.amount) || 0) / maxAmount) * 100)}%` }}
                />
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="overflow-x-auto rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h3 className="mb-4 text-lg font-semibold text-slate-800">Nhật ký giao dịch</h3>
        {(summary.recentTransactions || []).length === 0 ? (
          <p className="text-sm text-slate-500">Chưa có giao dịch.</p>
        ) : (
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-200">
              <tr>
                <th className="px-3 py-2">Thời gian</th>
                <th className="px-3 py-2">Loại</th>
                <th className="px-3 py-2">Mã</th>
                <th className="px-3 py-2">Nội dung</th>
                <th className="px-3 py-2">Thanh toán</th>
                <th className="px-3 py-2">Số tiền</th>
              </tr>
            </thead>
            <tbody>
              {summary.recentTransactions.map((row, index) => (
                <tr key={`${row.type}-${row.reference}-${index}`} className="border-t border-slate-100">
                  <td className="px-3 py-3">{formatDate(row.occurredAt)}</td>
                  <td className="px-3 py-3">
                    <span
                      className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
                        row.type === "REVENUE" ? "bg-emerald-100 text-emerald-700" : "bg-rose-100 text-rose-700"
                      }`}
                    >
                      {row.type === "REVENUE" ? "Doanh thu" : "Giá vốn sản xuất"}
                    </span>
                  </td>
                  <td className="px-3 py-3 font-medium">{row.reference}</td>
                  <td className="px-3 py-3">{row.description || "—"}</td>
                  <td className="px-3 py-3">{row.paymentMethod ? METHOD_LABELS[row.paymentMethod] || row.paymentMethod : "—"}</td>
                  <td className={`px-3 py-3 font-semibold ${row.type === "REVENUE" ? "text-emerald-700" : "text-rose-700"}`}>
                    {row.type === "REVENUE" ? "+" : "−"}
                    {formatPrice(row.amount)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
