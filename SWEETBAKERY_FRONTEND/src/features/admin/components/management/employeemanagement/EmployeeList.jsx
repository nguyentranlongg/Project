import React from "react";

const ROLE_LABELS = {
  SALES: "Nhân viên bán hàng",
  IMPORT_STAFF: "Nhân viên nhập kho / Bếp",
  INVENTORY_MANAGER: "Nhân viên quản lý kho",
  ACCOUNTANT: "Nhân viên kế toán",
  EMPLOYEE: "Nhân viên chung",
  ADMIN: "Quản trị viên",
};

const ROLE_BADGE_CLASS = {
  SALES: "bg-sky-100 text-sky-800",
  IMPORT_STAFF: "bg-amber-100 text-amber-800",
  INVENTORY_MANAGER: "bg-violet-100 text-violet-800",
  ACCOUNTANT: "bg-emerald-100 text-emerald-800",
  EMPLOYEE: "bg-slate-100 text-slate-700",
  ADMIN: "bg-rose-100 text-rose-800",
};

export default function EmployeeList({ users, onEdit, onDelete }) {
  return (
    <div className="overflow-x-auto rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <table className="w-full text-left text-sm">
        <thead className="border-b border-slate-200">
          <tr>
            <th className="px-4 py-3 font-semibold text-slate-600">Tên</th>
            {/* 1. Thêm cột Username */}
            <th className="px-4 py-3 font-semibold text-slate-600">Username</th>
            <th className="px-4 py-3 font-semibold text-slate-600">Email</th>
            <th className="px-4 py-3 font-semibold text-slate-600">SĐT</th>
            <th className="px-4 py-3 font-semibold text-slate-600">Vai trò</th>
            {/* 2. Thêm cột Trạng thái */}
            <th className="px-4 py-3 font-semibold text-slate-600">Trạng thái</th>
            <th className="px-4 py-3 font-semibold text-slate-600">Hành động</th>
          </tr>
        </thead>
        <tbody>
          {users.length > 0 ? (
            users.map((user) => (
              <tr key={user.id} className="border-t border-slate-100 hover:bg-slate-50">
                <td className="px-4 py-3 font-medium text-slate-800">{user.fullName}</td>

                {/* Hiển thị Username */}
                <td className="px-4 py-3 text-slate-600 font-medium">{user.username}</td>

                <td className="px-4 py-3 text-slate-600">{user.email}</td>
                <td className="px-4 py-3 text-slate-600">{user.phone}</td>
                <td className="px-4 py-3">
                  <div className="flex flex-wrap gap-1">
                    {(user.roles?.length ? user.roles : [user.roleName]).filter(Boolean).map((name) => (
                      <span
                        key={name}
                        className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium ${ROLE_BADGE_CLASS[name] || "bg-slate-100 text-slate-700"}`}
                      >
                        {ROLE_LABELS[name] || name}
                      </span>
                    ))}
                  </div>
                </td>

                {/* Hiển thị Trạng thái (Màu sắc theo isVerified) */}
                <td className="px-4 py-3">
                  <span
                    className={`inline-flex rounded-full px-3 py-1 text-xs font-medium ${
                      user.isVerified ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700"
                    }`}
                  >
                    {user.isVerified ? "Đã kích hoạt" : "Vô hiệu hóa"}
                  </span>
                </td>

                <td className="px-4 py-3">
                  <div className="flex items-center gap-3">
                    <button
                      onClick={() => onEdit(user)}
                      className="rounded-lg bg-amber-100 px-3 py-1.5 text-sm text-amber-700 transition hover:bg-amber-200"
                    >
                      Sửa
                    </button>
                    <button
                      onClick={() => onDelete(user.id)}
                      className="rounded-lg bg-red-100 px-3 py-1.5 text-sm text-red-700 transition hover:bg-red-200"
                    >
                      Xóa
                    </button>
                  </div>
                </td>
              </tr>
            ))
          ) : (
            <tr>
              {/* Cập nhật colSpan thành 7 vì đã thêm 2 cột mới */}
              <td colSpan="7" className="py-4 text-center text-slate-500">
                Chưa có dữ liệu
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
