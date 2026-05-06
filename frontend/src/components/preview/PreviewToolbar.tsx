interface PreviewToolbarProps {
  currentPage: number;
  totalPages: number;
  zoom: number;
  onPageChange: (page: number) => void;
  onZoomChange: (zoom: number) => void;
  onFitWidth: () => void;
  onFitPage: () => void;
}

export default function PreviewToolbar({
  currentPage,
  totalPages,
  zoom,
  onPageChange,
  onZoomChange,
  onFitWidth,
  onFitPage,
}: PreviewToolbarProps) {
  const zoomPercent = Math.round(zoom * 100);

  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      gap: 12,
      padding: '8px 16px',
      background: '#1f2937',
      borderBottom: '1px solid #374151',
      color: '#f9fafb',
      fontSize: 14,
    }}>
      {/* Page navigation */}
      {totalPages > 1 && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <button
            onClick={() => onPageChange(Math.max(1, currentPage - 1))}
            disabled={currentPage <= 1}
            style={buttonStyle}
          >
            ◀
          </button>
          <span>
            {currentPage} / {totalPages}
          </span>
          <button
            onClick={() => onPageChange(Math.min(totalPages, currentPage + 1))}
            disabled={currentPage >= totalPages}
            style={buttonStyle}
          >
            ▶
          </button>
        </div>
      )}

      {/* Separator */}
      {totalPages > 1 && <div style={{ width: 1, height: 20, background: '#4b5563' }} />}

      {/* Zoom controls */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <button
          onClick={() => onZoomChange(Math.max(0.25, zoom - 0.25))}
          disabled={zoom <= 0.25}
          style={buttonStyle}
        >
          −
        </button>
        <span style={{ minWidth: 50, textAlign: 'center' }}>{zoomPercent}%</span>
        <button
          onClick={() => onZoomChange(Math.min(4, zoom + 0.25))}
          disabled={zoom >= 4}
          style={buttonStyle}
        >
          +
        </button>
      </div>

      {/* Fit controls */}
      <div style={{ width: 1, height: 20, background: '#4b5563' }} />
      <button onClick={onFitWidth} style={buttonStyle} title="Fit to width">
        ↔
      </button>
      <button onClick={onFitPage} style={buttonStyle} title="Fit to page">
        ⊡
      </button>
    </div>
  );
}

const buttonStyle: React.CSSProperties = {
  background: '#374151',
  border: '1px solid #4b5563',
  borderRadius: 4,
  color: '#f9fafb',
  padding: '4px 10px',
  cursor: 'pointer',
  fontSize: 14,
  lineHeight: 1,
};
