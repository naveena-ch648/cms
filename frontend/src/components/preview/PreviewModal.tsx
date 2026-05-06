import { useEffect, useState, useCallback } from 'react';
import type { PreviewData } from '../../types/preview';
import { getPreview } from '../../api/previews';
import PdfViewer from './PdfViewer';
import ImageViewer from './ImageViewer';
import VideoPlayer from './VideoPlayer';
import OfficeViewer from './OfficeViewer';
import PreviewToolbar from './PreviewToolbar';
import MetadataPanel from './MetadataPanel';

interface PreviewModalProps {
  fileId: string;
  fileName: string;
  mimeType: string;
  onClose: () => void;
}

export default function PreviewModal({ fileId, fileName, mimeType, onClose }: PreviewModalProps) {
  const [preview, setPreview] = useState<PreviewData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [zoom, setZoom] = useState(1.0);
  const [showMetadata, setShowMetadata] = useState(false);

  useEffect(() => {
    let cancelled = false;

    const loadPreview = async () => {
      try {
        setLoading(true);
        const data = await getPreview(fileId);
        if (!cancelled) {
          setPreview(data);
          if (data.pageCount > 0) {
            setTotalPages(data.pageCount);
          }
        }
      } catch (err: any) {
        if (!cancelled) {
          setError(err.response?.data?.error?.message || 'Failed to load preview');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    loadPreview();
    return () => { cancelled = true; };
  }, [fileId]);

  const handleKeyDown = useCallback((e: KeyboardEvent) => {
    if (e.key === 'Escape') onClose();
    if (e.key === 'ArrowLeft') setCurrentPage(p => Math.max(1, p - 1));
    if (e.key === 'ArrowRight') setCurrentPage(p => Math.min(totalPages, p + 1));
  }, [onClose, totalPages]);

  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown]);

  const handleFitWidth = () => setZoom(1.0);
  const handleFitPage = () => setZoom(0.75);

  const renderViewer = () => {
    if (loading) {
      return <div style={{ padding: 40, textAlign: 'center', color: '#9ca3af' }}>Loading preview...</div>;
    }

    if (error) {
      return (
        <div style={{ padding: 40, textAlign: 'center' }}>
          <p style={{ color: '#ef4444', marginBottom: 16 }}>{error}</p>
          <a href={preview?.directUrl || '#'} download={fileName} style={{ color: '#3b82f6' }}>
            Download file instead
          </a>
        </div>
      );
    }

    if (!preview || preview.status === 'PENDING' || preview.status === 'PROCESSING') {
      return (
        <div style={{ padding: 40, textAlign: 'center', color: '#9ca3af' }}>
          Preview is being generated... Please check back shortly.
        </div>
      );
    }

    if (preview.status === 'FAILED') {
      return (
        <div style={{ padding: 40, textAlign: 'center', color: '#ef4444' }}>
          Preview generation failed. Try downloading the file.
        </div>
      );
    }

    // Route to appropriate viewer based on mime type
    if (mimeType === 'application/pdf') {
      if (preview.directUrl) {
        return (
          <PdfViewer
            url={preview.directUrl}
            zoom={zoom}
            currentPage={currentPage}
            onPageCountChange={setTotalPages}
          />
        );
      }
      if (preview.pages && preview.pages.length > 0) {
        return <OfficeViewer pages={preview.pages} currentPage={currentPage} zoom={zoom} />;
      }
    }

    if (mimeType.startsWith('image/')) {
      return <ImageViewer url={preview.directUrl || ''} zoom={zoom} onZoomChange={setZoom} />;
    }

    if (mimeType.startsWith('video/')) {
      return <VideoPlayer url={preview.directUrl || ''} />;
    }

    // Office documents - rendered as page images
    if (isOfficeMimeType(mimeType)) {
      if (preview.pages && preview.pages.length > 0) {
        return <OfficeViewer pages={preview.pages} currentPage={currentPage} zoom={zoom} />;
      }
    }

    return (
      <div style={{ padding: 40, textAlign: 'center', color: '#9ca3af' }}>
        Preview not available for this file type.
      </div>
    );
  };

  return (
    <div style={{
      position: 'fixed',
      inset: 0,
      zIndex: 1000,
      display: 'flex',
      flexDirection: 'column',
      background: '#111827',
    }}>
      {/* Header */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '8px 16px',
        background: '#1f2937',
        borderBottom: '1px solid #374151',
      }}>
        <h3 style={{ color: '#f9fafb', margin: 0, fontSize: 16, fontWeight: 500 }}>{fileName}</h3>
        <div style={{ display: 'flex', gap: 8 }}>
          <button
            onClick={() => setShowMetadata(!showMetadata)}
            style={{ background: showMetadata ? '#3b82f6' : '#374151', border: 'none', borderRadius: 4, color: '#f9fafb', padding: '6px 12px', cursor: 'pointer' }}
          >
            ℹ Info
          </button>
          <button
            onClick={onClose}
            style={{ background: '#374151', border: 'none', borderRadius: 4, color: '#f9fafb', padding: '6px 12px', cursor: 'pointer', fontSize: 16 }}
          >
            ✕
          </button>
        </div>
      </div>

      {/* Toolbar */}
      {(mimeType === 'application/pdf' || isOfficeMimeType(mimeType)) && (
        <PreviewToolbar
          currentPage={currentPage}
          totalPages={totalPages}
          zoom={zoom}
          onPageChange={setCurrentPage}
          onZoomChange={setZoom}
          onFitWidth={handleFitWidth}
          onFitPage={handleFitPage}
        />
      )}

      {/* Content area */}
      <div style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>
        <div style={{ flex: 1, overflow: 'auto', position: 'relative' }}>
          {renderViewer()}
        </div>

        {/* Side panel */}
        {showMetadata && (
          <MetadataPanel fileId={fileId} onClose={() => setShowMetadata(false)} />
        )}
      </div>
    </div>
  );
}

function isOfficeMimeType(mimeType: string): boolean {
  return [
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    'application/msword',
    'application/vnd.ms-excel',
    'application/vnd.ms-powerpoint',
  ].includes(mimeType);
}
