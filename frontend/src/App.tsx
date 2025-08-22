// src/App.tsx
import React, { createContext, useContext, useEffect, useMemo, useRef, useState } from "react";
import {
  QueryClient,
  QueryClientProvider,
  useMutation,
  useQuery,
  useQueryClient,
  keepPreviousData,
} from "@tanstack/react-query";
import {
  addTag,
  createPaper,
  deletePaper,
  listPapers,
  removeTag,
  updateStatus,
  updatePaper,
  type PaperRes,
  type Status,
} from "./api";
import "./styles.css";

/* ========== 小さなユーティリティ ========== */
function useDebounced<T>(value: T, delay = 350) {
  const [v, setV] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setV(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return v;
}
const fmt = (ts?: number) => (ts ? new Date(ts).toLocaleString() : "-");

/* ========== トースト（シンプル） ========== */
type Toast = { id: number; text: string };
const ToastCtx = createContext<{ push: (text: string) => void }>({ push: () => {} });
function ToastHost({ children }: { children: React.ReactNode }) {
  const [items, setItems] = useState<Toast[]>([]);
  const push = (text: string) => {
    const id = Date.now() + Math.random();
    setItems((x) => [...x, { id, text }]);
    setTimeout(() => setItems((x) => x.filter((i) => i.id !== id)), 2400);
  };
  return (
    <ToastCtx.Provider value={{ push }}>
      {children}
      <div className="toast-host">
        {items.map((t) => (
          <div key={t.id} className="toast">{t.text}</div>
        ))}
      </div>
    </ToastCtx.Provider>
  );
}
const useToast = () => useContext(ToastCtx);

/* ========== アプリ本体 ========== */
const qc = new QueryClient();
export default function App() {
  return (
    <QueryClientProvider client={qc}>
      <ToastHost>
        <Shell />
      </ToastHost>
    </QueryClientProvider>
  );
}

function Shell() {
  // URL 初期値
  const init = new URLSearchParams(location.search);
  const [page, setPage] = useState<number>(parseInt(init.get("page") || "0", 10) || 0);
  const [size, setSize] = useState<number>(parseInt(init.get("size") || "10", 10) || 10);
  const [qInput, setQInput] = useState(init.get("q") || "");
  const [status, setStatus] = useState(init.get("status") || "");
  const [tagsText, setTagsText] = useState(init.getAll("tags").join(", "));

  const q = useDebounced(qInput, 350);
  const tags = useMemo(
    () => tagsText.split(",").map((s) => s.trim().toLowerCase()).filter(Boolean),
    [tagsText]
  );

  // URL 同期
  useEffect(() => {
    const sp = new URLSearchParams();
    if (q) sp.set("q", q);
    if (status) sp.set("status", status);
    tags.forEach((t) => sp.append("tags", t));
    if (page) sp.set("page", String(page));
    if (size !== 10) sp.set("size", String(size));
    history.replaceState(null, "", sp.toString() ? `?${sp.toString()}` : location.pathname);
  }, [q, status, tags, page, size]);

  // ダークモード
  const [dark, setDark] = useState(() => document.documentElement.classList.contains("theme-dark"));
  useEffect(() => {
    document.documentElement.classList.toggle("theme-dark", dark);
  }, [dark]);

  // 検索欄ショートカット
  const searchRef = useRef<HTMLInputElement>(null);
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        searchRef.current?.focus();
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  return (
    <div className="container">
      <header className="header">
        <div className="brand">
          <div className="logo" />
          <h1>Paper Manager</h1>
        </div>
        <div className="right">
          <span className="hint">Backend:</span>
          <code>http://localhost:8080</code>
          <button className="btn outline" onClick={() => setDark((d) => !d)}>
            {dark ? "☀️" : "🌙"}
          </button>
        </div>
      </header>

      <Toolbar
        searchRef={searchRef}
        qInput={qInput}
        setQInput={(v) => { setQInput(v); setPage(0); }}
        status={status}
        setStatus={(v) => { setStatus(v); setPage(0); }}
        tagsText={tagsText}
        setTagsText={(v) => { setTagsText(v); setPage(0); }}
        size={size}
        setSize={(v) => { setSize(v); setPage(0); }}
      />

      <PapersTable
        page={page} size={size} q={q} status={status} tags={tags}
        onPrev={() => setPage((p) => Math.max(0, p - 1))}
        onNext={() => setPage((p) => p + 1)}
      />
    </div>
  );
}

function Toolbar(props: {
  searchRef: React.MutableRefObject<HTMLInputElement | null>;
  qInput: string; setQInput: (v: string) => void;
  status: string; setStatus: (v: string) => void;
  tagsText: string; setTagsText: (v: string) => void;
  size: number; setSize: (v: number) => void;
}) {
  const [open, setOpen] = useState(false);
  const { searchRef, qInput, setQInput, status, setStatus, tagsText, setTagsText, size, setSize } = props;

  return (
    <div className="card toolbar">
      <div className="grid">
        <div className="icon-input">
          <span className="icon">🔎</span>
          <input ref={searchRef} value={qInput} onChange={(e) => setQInput(e.target.value)}
                 placeholder="キーワード (title/authors)  —  ⌘/Ctrl + K でフォーカス" />
        </div>
        <div className="icon-input">
          <span className="icon">⛳</span>
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">status: ALL</option>
            <option value="UNREAD">UNREAD</option>
            <option value="READING">READING</option>
            <option value="DONE">DONE</option>
          </select>
        </div>
        <div className="icon-input">
          <span className="icon">🏷️</span>
          <input value={tagsText} onChange={(e) => setTagsText(e.target.value)}
                 placeholder="tags (comma separated)" />
        </div>
        <select value={size} onChange={(e) => setSize(Number(e.target.value))}>
          {[10, 20, 50].map((n) => <option key={n} value={n}>{n}/page</option>)}
        </select>
        <button className="btn primary" onClick={() => setOpen(true)}>＋ 新規作成</button>
      </div>
      {open && <CreateModal onClose={() => setOpen(false)} />}
    </div>
  );
}

function CreateModal({ onClose }: { onClose: () => void }) {
  const { push } = useToast();
  const qc = useQueryClient();
  const [title, setTitle] = useState("");
  const [authors, setAuthors] = useState("");
  const [year, setYear] = useState("");
  const [url, setUrl] = useState("");
  const [err, setErr] = useState<string | null>(null);

  const mut = useMutation({
    mutationFn: () =>
      createPaper({
        title: title.trim(),
        authors: authors || undefined,
        year: year ? Number(year) : undefined,
        url: url || undefined,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["papers"] });
      push("作成しました");
      onClose();
    },
    onError: (e: any) => setErr(e?.response?.data?.message || e?.message || "failed"),
  });

  const submit = () => {
    if (!title.trim()) return setErr("title は必須");
    if (year && (+year < 1900 || +year > 2100)) return setErr("year は 1900-2100");
    mut.mutate();
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <header className="modal-head">
          <h3>新規作成</h3>
          <button className="btn outline" onClick={onClose}>×</button>
        </header>
        <div className="modal-grid">
          <input placeholder="title *" value={title} onChange={(e) => setTitle(e.target.value)} />
          <input placeholder="authors" value={authors} onChange={(e) => setAuthors(e.target.value)} />
          <input placeholder="year" value={year} onChange={(e) => setYear(e.target.value)} />
          <input placeholder="url" value={url} onChange={(e) => setUrl(e.target.value)} />
        </div>
        {err && <div className="error">{err}</div>}
        <footer className="modal-foot">
          <button className="btn" onClick={onClose}>キャンセル</button>
          <button className="btn primary" onClick={submit} disabled={mut.isPending}>
            {mut.isPending ? "作成中…" : "作成"}
          </button>
        </footer>
      </div>
    </div>
  );
}

function EditModal({ paper, onClose }: { paper: PaperRes; onClose: () => void }) {
  const qc = useQueryClient();
  const { push } = useToast();

  // 既存値を初期表示。空文字にすると「未指定」で送らない運用にしやすい。
  const [title, setTitle] = useState(paper.title ?? "");
  const [authors, setAuthors] = useState(paper.authors ?? "");
  const [year, setYear] = useState(paper.year ? String(paper.year) : "");
  const [url, setUrl] = useState(paper.url ?? "");
  const [err, setErr] = useState<string | null>(null);

  // 変更点だけ投げる（未入力＝送らない）
  const buildPayload = () => {
    const payload: any = {};
    if (title.trim() && title.trim() !== paper.title) payload.title = title.trim();
    if (authors.trim() !== (paper.authors ?? ""))     payload.authors = authors.trim() || undefined;
    if (year) {
      const y = Number(year);
      if (Number.isNaN(y) || y < 1900 || y > 2100) {
        setErr("year は 1900-2100");
        return null;
      }
      if (paper.year !== y) payload.year = y;
    }
    if (url.trim() !== (paper.url ?? "")) payload.url = url.trim() || undefined;
    return payload;
  };

  const mut = useMutation({
    mutationFn: (payload: any) => updatePaper(paper.id, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["papers"] });
      push("更新しました");
      onClose();
    },
    onError: (e: any) => setErr(e?.response?.data?.message || e?.message || "failed"),
  });

  const submit = () => {
    if (!title.trim()) return setErr("title は必須"); // タイトルだけは必須運用
    const payload = buildPayload();
    if (!payload) return; // year バリデーションで中断
    // 変更が無ければ何もしない
    if (Object.keys(payload).length === 0) { onClose(); return; }
    mut.mutate(payload);
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <header className="modal-head">
          <h3>編集: #{paper.id}</h3>
          <button className="btn outline" onClick={onClose}>×</button>
        </header>
        <div className="modal-grid">
          <input placeholder="title *"  value={title}   onChange={(e) => setTitle(e.target.value)} />
          <input placeholder="authors"  value={authors} onChange={(e) => setAuthors(e.target.value)} />
          <input placeholder="year"     value={year}    onChange={(e) => setYear(e.target.value)} />
          <input placeholder="url"      value={url}     onChange={(e) => setUrl(e.target.value)} />
        </div>
        {err && <div className="error">{err}</div>}
        <footer className="modal-foot">
          <button className="btn" onClick={onClose}>キャンセル</button>
          <button className="btn primary" onClick={submit} disabled={mut.isPending}>
            {mut.isPending ? "保存中…" : "保存"}
          </button>
        </footer>
      </div>
    </div>
  );
}

function PapersTable(props: {
  page: number; size: number; q: string; status: string; tags: string[];
  onPrev: () => void; onNext: () => void;
}) {
  const { push } = useToast();
  const qc = useQueryClient();
  const { page, size, q, status, tags, onPrev, onNext } = props;
  const queryKey = ["papers", { page, size, q, status, tags }];
  const [editing, setEditing] = useState<PaperRes | null>(null);
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey,
    queryFn: () => listPapers({ page, size, q: q || undefined, status: status || undefined, tags: tags.length ? tags : undefined }),
    placeholderData: keepPreviousData,
  });

  const statusMut = useMutation({
    mutationFn: ({ id, st }: { id: number; st: Status }) => updateStatus(id, st),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["papers"] }); push("ステータス更新"); },
  });

  const tagAdd = useMutation({
    mutationFn: ({ id, tag }: { id: number; tag: string }) => addTag(id, tag),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["papers"] }); push("タグ追加"); },
  });
  const tagRemove = useMutation({
    mutationFn: ({ id, tag }: { id: number; tag: string }) => removeTag(id, tag),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["papers"] }); push("タグ削除"); },
  });

  const delMut = useMutation({
    mutationFn: (id: number) => deletePaper(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["papers"] }); push("削除しました"); },
  });

  return (
    <>
      {isError && (
        <div className="alert">
          読み込みに失敗しました。<button className="link" onClick={() => refetch()}>再試行</button>
        </div>
      )}

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>ID</th><th>Title</th><th>Authors</th><th>Year</th><th>Status</th><th>URL</th><th>Tags</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              [...Array(6)].map((_, i) => (
                <tr key={i}><td colSpan={8}><div className="skeleton" /></td></tr>
              ))
            ) : data && data.content.length ? (
              data.content.map((p) => (
                <tr key={p.id}>
                  <td className="muted">{p.id}</td>
                  <td>
                    <div className="title">{p.title}</div>
                    <div className="muted tiny">{fmt(p.createdAt)}</div>
                  </td>
                  <td>{p.authors || "-"}</td>
                  <td className="center">{p.year ?? "-"}</td>
                  <td>
                    <div className="status">
                      <span className={`badge ${p.status.toLowerCase()}`}>{p.status}</span>
                      <div className="status-actions">
                        <button className="btn sm" onClick={() => statusMut.mutate({ id: p.id, st: "READING" })}>→ READING</button>
                        <button className="btn sm" onClick={() => statusMut.mutate({ id: p.id, st: "DONE" })}>→ DONE</button>
                        <button className="btn sm" onClick={() => statusMut.mutate({ id: p.id, st: "UNREAD" })}>Reset</button>
                      </div>
                    </div>
                  </td>
                  <td>
                    {p.url ? <a className="link" href={p.url} target="_blank" rel="noreferrer">{p.url}</a> : "-"}
                  </td>
                  <td>
                    <TagsCell
                      p={p}
                      onAdd={(t) => tagAdd.mutate({ id: p.id, tag: t })}
                      onRemove={(t) => tagRemove.mutate({ id: p.id, tag: t })}
                    />
                  </td>
                  <td className="center">
                    <button className="btn" onClick={() => setEditing(p)}>
                      Edit
                    </button>
                    <button
                      className="btn danger"
                      onClick={() => { if (confirm(`Delete #${p.id}?`)) delMut.mutate(p.id); }}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))
            ) : (
              <tr><td colSpan={8} className="empty">データがありません。左上の「新規作成」から追加してね。</td></tr>
            )}
          </tbody>
        </table>
      </div>
      
      {editing && (
        <EditModal
          paper={editing}
          onClose={() => setEditing(null)}
        />
      )}
      <div className="pager">
        <button className="btn" onClick={props.onPrev} disabled={!data || data.page === 0}>← Prev</button>
        <button className="btn" onClick={onPrev} disabled={!data || data.page === 0}>← Prev</button>
        <span>page {data ? data.page + 1 : 1}</span>
        <button className="btn" onClick={onNext} disabled={!data || !data.hasNext}>Next →</button>
      </div>
    </>
  );
}

function TagsCell({ p, onAdd, onRemove }: {
  p: PaperRes; onAdd: (t: string) => void; onRemove: (t: string) => void;
}) {
  const [t, setT] = useState("");
  return (
    <div className="tags-cell">
      <div className="tags">
        {p.tags?.length ? p.tags.map((tag) => (
          <span key={tag} className="tag">
            #{tag}
            <button aria-label="remove tag" onClick={() => onRemove(tag)}>×</button>
          </span>
        )) : <span className="muted tiny">(tags なし)</span>}
      </div>
      <div className="tag-add">
        <input
          placeholder="add tag"
          value={t}
          onChange={(e) => setT(e.target.value)}
          onKeyDown={(e) => { if (e.key === "Enter" && t.trim()) { onAdd(t.trim()); setT(""); } }}
        />
        <button className="btn" onClick={() => { if (t.trim()) { onAdd(t.trim()); setT(""); } }}>Add</button>
      </div>
    </div>
  );
}
