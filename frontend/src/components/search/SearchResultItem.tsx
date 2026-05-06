import type { SearchResult } from '../../types/search';

interface SearchResultItemProps {
  result: SearchResult;
  onClick?: (result: SearchResult) => void;
}

const FILE_TYPE_ICONS: Record<string, string> = {
  pdf: '📄',
  image: '🖼️',
  document: '📝',
  spreadsheet: '📊',
  presentation: '📽️',
  video: '🎬',
  audio: '🎵',
  archive: '📦',
  other: '📁',
};

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`;
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

export default function SearchResultItem({ result, onClick }: SearchResultItemProps) {
  const icon = FILE_TYPE_ICONS[result.fileType] || FILE_TYPE_ICONS.other;

  return (
    <div
      className="search-result-item"
      onClick={() => onClick?.(result)}
      style={{
        padding: '12px 16px',
        borderBottom: '1px solid #e5e7eb',
        cursor: 'pointer',
        display: 'flex',
        gap: '12px',
        alignItems: 'flex-start',
      }}
    >
      <span style={{ fontSize: '24px', flexShrink: 0 }}>{icon}</span>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h4 style={{ margin: 0, fontSize: '14px', fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {result.fileName}
          </h4>
          <span style={{ fontSize: '12px', color: '#6b7280', flexShrink: 0, marginLeft: '8px' }}>
            {formatFileSize(result.fileSize)}
          </span>
        </div>
        <div style={{ fontSize: '12px', color: '#6b7280', marginTop: '2px' }}>
          {result.folderPath} • {result.ownerName} • {formatDate(result.updatedAt)}
        </div>
        {result.highlights && result.highlights.length > 0 && result.highlights[0] && (
          <div
            style={{ fontSize: '13px', color: '#374151', marginTop: '6px', lineHeight: 1.4 }}
            dangerouslySetInnerHTML={{ __html: result.highlights[0] }}
          />
        )}
      </div>
    </div>
  );
}
