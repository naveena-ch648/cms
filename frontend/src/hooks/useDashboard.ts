import { useState, useCallback, useRef } from "react";
import { usePolling } from "./usePolling";
import {
  getDashboardSummary,
  getRecentFiles,
  getActivity,
  getSharedItems,
  getAlerts,
  dismissAlert as apiDismissAlert,
} from "../api/dashboard";
import { notificationsApi } from "../api/notifications";
import type {
  DashboardSummary,
  RecentFile,
  ActivityEvent,
  SharedItem,
  Alert,
} from "../types/dashboard";
import type { NotificationItem } from "../types/collaboration";

export interface DashboardData {
  summary: DashboardSummary | null;
  recentFiles: RecentFile[];
  activity: ActivityEvent[];
  sharedItems: SharedItem[];
  alerts: Alert[];
  notifications: NotificationItem[];
  unreadCount: number;

  summaryLoading: boolean;
  filesLoading: boolean;
  activityLoading: boolean;
  sharedLoading: boolean;
  alertsLoading: boolean;
  notifLoading: boolean;

  summaryError: string | null;
  filesError: string | null;
  activityError: string | null;

  activityHasMore: boolean;
  sharedDirection: "WITH_ME" | "BY_ME";
  lastUpdated: Date | null;
  newActivityCount: number;

  setSharedDirection: (d: "WITH_ME" | "BY_ME") => void;
  loadMoreActivity: () => void;
  dismissAlert: (id: string) => void;
  markNotifRead: (id: string) => void;
  markAllNotifRead: () => void;
  refreshAll: () => void;
}

const SUMMARY_INTERVAL = 60_000; // 1 min
const FILES_INTERVAL = 60_000; // 1 min
const ACTIVITY_INTERVAL = 30_000; // 30 s
const NOTIF_INTERVAL = 30_000; // 30 s
const ALERTS_INTERVAL = 120_000; // 2 min
const SHARED_INTERVAL = 30_000; // 30 s

export function useDashboard(): DashboardData {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [recentFiles, setRecentFiles] = useState<RecentFile[]>([]);
  const [activity, setActivity] = useState<ActivityEvent[]>([]);
  const [sharedItems, setSharedItems] = useState<SharedItem[]>([]);
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);

  const [summaryLoading, setSummaryLoading] = useState(true);
  const [filesLoading, setFilesLoading] = useState(true);
  const [activityLoading, setActivityLoading] = useState(true);
  const [sharedLoading, setSharedLoading] = useState(true);
  const [alertsLoading, setAlertsLoading] = useState(true);
  const [notifLoading, setNotifLoading] = useState(true);

  const [summaryError, setSummaryError] = useState<string | null>(null);
  const [filesError, setFilesError] = useState<string | null>(null);
  const [activityError, setActivityError] = useState<string | null>(null);

  const [activityPage, setActivityPage] = useState(0);
  const [activityHasMore, setActivityHasMore] = useState(true);
  const [sharedDirection, setSharedDirection] = useState<"WITH_ME" | "BY_ME">(
    "WITH_ME",
  );
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const [newActivityCount, setNewActivityCount] = useState(0);

  const latestActivityIdRef = useRef<string | null>(null);
  // Keep a ref to current direction so the polling closure always reads the latest value
  const sharedDirectionRef = useRef<"WITH_ME" | "BY_ME">("WITH_ME");

  // ─── Fetchers ────────────────────────────────────────────────────────────

  const fetchSummary = useCallback(() => {
    getDashboardSummary()
      .then((data) => {
        setSummary(data);
        setSummaryError(null);
      })
      .catch(() => setSummaryError("Failed to load summary"))
      .finally(() => {
        setSummaryLoading(false);
        setLastUpdated(new Date());
      });
  }, []);

  const fetchFiles = useCallback(() => {
    getRecentFiles(10)
      .then((data) => {
        setRecentFiles(data);
        setFilesError(null);
      })
      .catch(() => setFilesError("Failed to load recent files"))
      .finally(() => setFilesLoading(false));
  }, []);

  const fetchActivity = useCallback((isBackground = false) => {
    if (!isBackground) setActivityLoading(true);
    getActivity(0, 15)
      .then((data) => {
        setActivityError(null);
        setActivity((prev) => {
          // count truly-new items
          if (latestActivityIdRef.current && prev.length > 0) {
            const newItems = data.content.filter(
              (e) => !prev.some((p) => p.id === e.id),
            );
            if (newItems.length > 0)
              setNewActivityCount((c) => c + newItems.length);
          }
          if (data.content.length > 0) {
            latestActivityIdRef.current = data.content[0]?.id ?? null;
          }
          return data.content;
        });
        setActivityHasMore(data.content.length === 15);
        setActivityPage(0);
      })
      .catch(() => setActivityError("Failed to load activity"))
      .finally(() => setActivityLoading(false));
  }, []);

  const fetchShared = useCallback((dir: "WITH_ME" | "BY_ME") => {
    setSharedLoading(true);
    getSharedItems(dir, 10)
      .then(setSharedItems)
      .catch(() => {})
      .finally(() => setSharedLoading(false));
  }, []);

  const fetchAlerts = useCallback(() => {
    getAlerts()
      .then(setAlerts)
      .catch(() => {})
      .finally(() => setAlertsLoading(false));
  }, []);

  const fetchNotifications = useCallback(() => {
    notificationsApi
      .getUnreadCount()
      .then((res) => setUnreadCount(res.data.data.unreadCount))
      .catch(() => {});
    notificationsApi
      .getNotifications({ page: 0, size: 15 })
      .then((res) => setNotifications(res.data.data))
      .catch(() => {})
      .finally(() => setNotifLoading(false));
  }, []);

  // ─── Initial load ─────────────────────────────────────────────────────────

  const refreshAll = useCallback(() => {
    fetchSummary();
    fetchFiles();
    fetchActivity();
    fetchNotifications();
    fetchAlerts();
  }, [
    fetchSummary,
    fetchFiles,
    fetchActivity,
    fetchNotifications,
    fetchAlerts,
  ]);

  // ─── Polling ──────────────────────────────────────────────────────────────

  usePolling(fetchSummary, SUMMARY_INTERVAL);
  usePolling(fetchFiles, FILES_INTERVAL);
  usePolling(() => fetchActivity(true), ACTIVITY_INTERVAL);
  usePolling(fetchNotifications, NOTIF_INTERVAL);
  usePolling(fetchAlerts, ALERTS_INTERVAL);

  // Re-fetch shared items when direction changes
  const handleSetSharedDirection = useCallback(
    (dir: "WITH_ME" | "BY_ME") => {
      sharedDirectionRef.current = dir;
      setSharedDirection(dir);
      fetchShared(dir);
    },
    [fetchShared],
  );

  // Poll shared items — always uses the ref so direction changes are picked up
  usePolling(() => fetchShared(sharedDirectionRef.current), SHARED_INTERVAL);

  // ─── Actions ──────────────────────────────────────────────────────────────

  const loadMoreActivity = useCallback(() => {
    const nextPage = activityPage + 1;
    setActivityPage(nextPage);
    getActivity(nextPage, 15)
      .then((data) => {
        setActivity((prev) => [...prev, ...data.content]);
        setActivityHasMore(data.content.length === 15);
      })
      .catch(() => {});
  }, [activityPage]);

  const dismissAlert = useCallback((id: string) => {
    apiDismissAlert(id).catch(() => {});
    setAlerts((prev) => prev.filter((a) => a.id !== id));
  }, []);

  const markNotifRead = useCallback((id: string) => {
    notificationsApi.markAsRead(id).catch(() => {});
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: true } : n)),
    );
    setUnreadCount((c) => Math.max(0, c - 1));
  }, []);

  const markAllNotifRead = useCallback(() => {
    notificationsApi.markAllAsRead().catch(() => {});
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
    setUnreadCount(0);
  }, []);

  return {
    summary,
    recentFiles,
    activity,
    sharedItems,
    alerts,
    notifications,
    unreadCount,
    summaryLoading,
    filesLoading,
    activityLoading,
    sharedLoading,
    alertsLoading,
    notifLoading,
    summaryError,
    filesError,
    activityError,
    activityHasMore,
    sharedDirection,
    lastUpdated,
    newActivityCount,
    setSharedDirection: handleSetSharedDirection,
    loadMoreActivity,
    dismissAlert,
    markNotifRead,
    markAllNotifRead,
    refreshAll,
  };
}
