import { useEffect, useState } from "react";
import adminApi from "../../apis/adminApi";

const ROLE_LABELS = {
  SALES: "Bán hàng",
  IMPORT_STAFF: "Nhập kho",
  INVENTORY_MANAGER: "Tồn kho",
  ACCOUNTANT: "Kế toán",
};

function formatNumber(value) {
  return Number(value || 0).toLocaleString("vi-VN");
}

function formatValue(role, value) {
  if (role === "INVENTORY_MANAGER") {
    return `${formatNumber(value)} sp`;
  }
  return formatNumber(value) + "₫";
}

export default function EmployeePerformanceView() {
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    adminApi
      .getEmployeePerformance()
      .then((res) => setReport(res.data?.data || null))
      .catch((err) => {
        console.error(err);
        alert("Không tải được đánh giá hiệu suất.");
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return <p className="text-sm text-slate-500">Đang tải...</p>;
  }
  if (!report) {
    return <p className="text-sm text-slate-500">Không có dữ liệu hiệu suất.</p>;
  }

  const staff = report.staff || [];
  const cards = [
    {
      title: "Bán hàng",
      bucket: report.sales,
      note: `Tỷ lệ đơn thanh toán QR/tiền mặt: ${formatNumber(report.sales?.metric)}%`,
    },
    {
      title: "Nhập kho",
      bucket: report.importStaff,
      note: `Sản phẩm đã nhập: ${formatNumber(report.importStaff?.metric)}`,
    },
    {
      title: "Điều chỉnh tồn kho",
      bucket: report.inventory,
      note: `Mặt hàng dưới ngưỡng đã xử lý: ${formatNumber(report.inventory?.metric)}`,
    },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-slate-800">Đánh giá hiệu suất</h2>
        <p className="text-sm text-slate-500">
          Hiệu suất giao hàng của cửa hàng: {formatNumber(report.fulfillmentRate)}% đơn đã thanh toán.
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        {cards.map((card) => (
          <div key={card.title} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <p className="text-sm text-slate-500">{card.title}</p>
            <p className="mt-2 text-2xl font-bold text-slate-800">{formatNumber(card.bucket?.totalTransactions)} giao dịch</p>
            <p className="mt-1 text-sm font-medium text-amber-700">{formatNumber(card.bucket?.totalValueManaged)}</p>
            <p className="mt-2 text-xs text-slate-500">{card.note}</p>
            <p className="text-xs text-slate-400">{formatNumber(card.bucket?.staffCount)} nhân sự</p>
          </div>
        ))}
      </div>

      <div className="overflow-x-auto rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h3 className="mb-4 text-lg font-semibold text-slate-800">Bảng xếp hạng</h3>
        {staff.length === 0 ? (
          <p className="text-sm text-slate-500">
            Chưa có nhân sự thuộc SALES, IMPORT_STAFF, INVENTORY_MANAGER hoặc ACCOUNTANT.
          </p>
        ) : (
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-200">
              <tr>
                <th className="px-3 py-2">Hạng</th>
                <th className="px-3 py-2">Nhân sự</th>
                <th className="px-3 py-2">Vai trò</th>
                <th className="px-3 py-2">Giao dịch</th>
                <th className="px-3 py-2">Giá trị</th>
                <th className="px-3 py-2">Điểm</th>
              </tr>
            </thead>
            <tbody>
              {staff.map((row, index) => (
                <tr key={row.employeeId} className="border-t border-slate-100">
                  <td className="px-3 py-3 font-semibold">{index + 1}</td>
                  <td className="px-3 py-3 font-medium">{row.fullName}</td>
                  <td className="px-3 py-3">{ROLE_LABELS[row.role] || row.role}</td>
                  <td className="px-3 py-3">{formatNumber(row.totalTransactions)}</td>
                  <td className="px-3 py-3">{formatValue(row.role, row.totalValueManaged)}</td>
                  <td className="px-3 py-3 font-semibold text-amber-700">{formatNumber(row.performanceRating)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
