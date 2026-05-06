import type { BreadcrumbItem } from '../types/folder';

interface BreadcrumbsProps {
  items: BreadcrumbItem[];
  workspaceName: string;
  onNavigate: (folderId: string | null) => void;
}

export default function Breadcrumbs({ items, workspaceName, onNavigate }: BreadcrumbsProps) {
  const maxVisible = 4;
  const showTruncation = items.length > maxVisible;
  const visibleItems = showTruncation
    ? [items[0], ...items.slice(items.length - (maxVisible - 1))]
    : items;

  return (
    <nav style={{ display: 'flex', alignItems: 'center', padding: '8px 16px', fontSize: 14, flexWrap: 'wrap' }}>
      <span
        onClick={() => onNavigate(null)}
        style={{ cursor: 'pointer', color: '#1976d2', textDecoration: 'none' }}
      >
        {workspaceName}
      </span>

      {items.length > 0 && <span style={{ margin: '0 6px', color: '#999' }}>/</span>}

      {showTruncation && (
        <>
          <span
            onClick={() => onNavigate(visibleItems[0]!.id)}
            style={{ cursor: 'pointer', color: '#1976d2' }}
          >
            {visibleItems[0]!.name}
          </span>
          <span style={{ margin: '0 6px', color: '#999' }}>/</span>
          <span style={{ color: '#999' }}>…</span>
          <span style={{ margin: '0 6px', color: '#999' }}>/</span>
          {visibleItems.slice(1).map((item, idx) => (
            <span key={item!.id}>
              {idx > 0 && <span style={{ margin: '0 6px', color: '#999' }}>/</span>}
              {idx === visibleItems.length - 2 ? (
                <span style={{ fontWeight: 600 }}>{item!.name}</span>
              ) : (
                <span
                  onClick={() => onNavigate(item!.id)}
                  style={{ cursor: 'pointer', color: '#1976d2' }}
                >
                  {item!.name}
                </span>
              )}
            </span>
          ))}
        </>
      )}

      {!showTruncation && items.map((item, idx) => (
        <span key={item.id}>
          {idx > 0 && <span style={{ margin: '0 6px', color: '#999' }}>/</span>}
          {idx === items.length - 1 ? (
            <span style={{ fontWeight: 600 }}>{item.name}</span>
          ) : (
            <span
              onClick={() => onNavigate(item.id)}
              style={{ cursor: 'pointer', color: '#1976d2' }}
            >
              {item.name}
            </span>
          )}
        </span>
      ))}
    </nav>
  );
}
