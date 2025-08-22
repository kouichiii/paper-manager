// src/api.ts
import axios from "axios";

export type Status = "UNREAD" | "READING" | "DONE";

export type PaperRes = {
  id: number;
  title: string;
  authors?: string;
  year?: number;
  url?: string;
  createdAt: number;
  status: Status;
  tags: string[]; // バックエンドで返すようにした前提
};

export type PageRes<T> = {
  content: T[];
  total: number;
  page: number;
  size: number;
  hasNext: boolean;
};

// Vite の proxy を使う前提（vite.config.ts の server.proxy を参照）
export const api = axios.create({
  baseURL: "",
  // 配列パラメータは ?tags=a&tags=b の形式に
  paramsSerializer: (params) => {
    const sp = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (Array.isArray(v)) v.forEach((x) => sp.append(k, String(x)));
      else if (v !== undefined && v !== null) sp.append(k, String(v));
    });
    return sp.toString();
  },
});

export async function listPapers(params: {
  page?: number; size?: number; q?: string; status?: string; tags?: string[];
}): Promise<PageRes<PaperRes>> {
  const res = await api.get("/api/papers", { params });
  return res.data;
}

export async function createPaper(req: {
  title: string; authors?: string; year?: number; url?: string;
}): Promise<PaperRes> {
  const res = await api.post("/api/papers", req);
  return res.data;
}

export async function updateStatus(id: number, status: Status): Promise<PaperRes> {
  const res = await api.patch(`/api/papers/${id}/status`, { status });
  return res.data;
}

export async function addTag(id: number, tag: string): Promise<PaperRes> {
  const res = await api.post(`/api/papers/${id}/tags`, { tag });
  return res.data;
}

export async function removeTag(id: number, tag: string): Promise<void> {
  await api.delete(`/api/papers/${id}/tags/${encodeURIComponent(tag)}`);
}

export async function deletePaper(id: number): Promise<void> {
  await api.delete(`/api/papers/${id}`);
}

export async function updatePaper(
  id: number,
  req: { title?: string; authors?: string; year?: number; url?: string }
): Promise<PaperRes> {
  const res = await api.patch(`/api/papers/${id}`, req);
  return res.data;
}