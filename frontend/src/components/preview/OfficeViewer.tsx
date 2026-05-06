import { useState } from 'react';
import type { PreviewPage } from '../../types/preview';

interface OfficeViewerProps {
  pages: PreviewPage[];
  currentPage: number;
  zoom: number;
}

export default function OfficeViewer({ pages, currentPage, zoom }: OfficeViewerProps) {
  const [loadError, setLoadError] = useState<Set<number>>(new Set());

  const page = pages.find(p => p.page === currentPage) || pages[0];

  if (!page) {
    return (
      <div style={{ padding: 40, textAlign: 'center', color: '#9ca3af' }}>
        No preview pages available
      </div>
    );
  }

  if (loadError.has(currentPage)) {
    return (
      <div style={{ padding: 40, textAlign: 'center', color: '#ef4444' }}>
        Failed to load page {currentPage}
      </div>
    );
  }

  return (
    <div style={{
      width: '100%',
      height: '100%',
      display: 'flex',
      alignItems: 'flex-start',
      justifyContent: 'center',
      overflow: 'auto',
      padding: 16,
    }}>
      <img
        src={page.url}
        alt={`Page ${currentPage}`}
        style={{
          transform: `scale(${zoom})`,
          transformOrigin: 'top center',
          maxWidth: '100%',
          boxShadow: '0 2px 8px rgba(0,0,0,0.3)',
        }}
        onError={() => setLoadError(prev => new Set(prev).add(currentPage))}
        draggable={false}
      />
    </div>
  );
}
