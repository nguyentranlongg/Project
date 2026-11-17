import React, { useEffect, useState } from "react";
import { MenuIcon } from "./icons";
import { useNavigate } from "react-router-dom";
import authApi from "../../auth/apis/authApi";
import adminApi from "../apis/adminApi";

const WAREHOUSE_ROLES = ["INVENTORY_MANAGER", "IMPORT_STAFF", "ADMIN"];

export default function HeaderBar({ onToggleSidebar }) {
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const [userInfo, setUserInfo] = useState(null);
  const [notifications, setNotifications] = useState([]);
  const [bellOpen, setBellOpen] = useState(false);

  useEffect(() => {
    const fetchUserInfo = async () => {
      const token = localStorage.getItem("access_token");
      if (!token) {
        navigate("/admin");
        return;
      }

      try {
        const res = await authApi.getInformation();
        if (res.data?.code === 200 && res.data?.data) {
          setUserInfo(res.data.data);
          console.log("User info:", res.data.data);
        } else {
          navigate("/admin");
        }
      } catch (err) {
        console.error("Failed to fetch user info:", err);
        navigate("/admin");
      } finally {
        setLoading(false);
      }
    };

    fetchUserInfo();
  }, [navigate]);

  const roleNames = (userInfo?.roles || []).map((role) => role.name);
  const canSeeWarehouseAlerts = roleNames.some((role) => WAREHOUSE_ROLES.includes(role));

  useEffect(() => {
    if (!canSeeWarehouseAlerts) {
      return undefined;
    }
    let active = true;
    const load = async () => {
      try {
        const res = await adminApi.getWarehouseNotifications();
        if (active) {
          setNotifications(res.data?.data || []);
        }
      } catch (err) {
        console.error(err);
      }
    };
    load();
    const timer = setInterval(load, 30000);
    return () => {
      active = false;
      clearInterval(timer);
    };
  }, [canSeeWarehouseAlerts]);

  const unread = notifications.filter((item) => !item.acknowledged);

  const markRead = async (item) => {
    if (item.acknowledged) {
      return;
    }
    try {
      await adminApi.acknowledgeWarehouseNotification(item.id);
      setNotifications((current) =>
        current.map((row) => (row.id === item.id ? { ...row, acknowledged: true } : row))
      );
    } catch (err) {
      console.error(err);
    }
  };

  // Lấy role name (mặc định là ADMIN nếu không có)
  const roleName = userInfo?.roles?.[0]?.name || "ADMIN";
  const roleDescription = userInfo?.roles?.[0]?.description || "Quản trị viên";
  const fullName = userInfo ? `${userInfo.username}` : "User";

  return (
    <header className="flex h-16 items-center justify-between border-b border-slate-200 bg-white px-6 shadow-sm">
      <button
        onClick={onToggleSidebar}
        className="rounded-lg p-2 text-slate-600 hover:bg-slate-100 lg:hidden"
      >
        <MenuIcon className="h-6 w-6" />
      </button>

      <div className="flex-1" />

      <div className="flex items-center gap-4">
        {canSeeWarehouseAlerts && (
          <div className="relative">
            <button
              type="button"
              onClick={() => setBellOpen((open) => !open)}
              className="relative rounded-lg p-2 text-slate-600 hover:bg-slate-100"
              aria-label="Thông báo kho"
            >
              <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.4-1.4A2 2 0 0118 14.2V11a6 6 0 10-12 0v3.2c0 .5-.2 1-.6 1.4L4 17h5m6 0a3 3 0 11-6 0m6 0H9" />
              </svg>
              {unread.length > 0 && (
                <span className="absolute right-1 top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-600 px-1 text-[10px] font-bold text-white">
                  {unread.length}
                </span>
              )}
            </button>
            {bellOpen && (
              <div className="absolute right-0 z-50 mt-2 w-80 rounded-xl border border-slate-200 bg-white p-3 shadow-lg">
                <p className="mb-2 text-sm font-semibold text-slate-800">Thông báo kho</p>
                {notifications.length === 0 ? (
                  <p className="text-sm text-slate-500">Không có cảnh báo.</p>
                ) : (
                  <div className="max-h-80 space-y-2 overflow-y-auto">
                    {notifications.map((item) => (
                      <button
                        key={item.id}
                        type="button"
                        onClick={() => markRead(item)}
                        className={`w-full rounded-lg px-3 py-2 text-left text-sm ${
                          item.acknowledged ? "text-slate-400" : "bg-amber-50 text-slate-800"
                        }`}
                      >
                        {item.message}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        <div className="flex items-center gap-3 border-l border-slate-200 pl-4">
          <div className="text-right">
            <p className="text-xs text-slate-500">{roleName}</p>
            <p className="text-sm font-medium text-slate-800">{fullName}</p>
          </div>
          <img
            src={`https://ui-avatars.com/api/?name=${fullName}&background=amber&color=fff`}
            alt="Avatar"
            className="h-8 w-8 rounded-full"
          />
        </div>
      </div>
    </header>
  );
}