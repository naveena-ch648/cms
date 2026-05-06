import { useRef, useState, useCallback } from 'react';

interface ImageViewerProps {
  url: string;
  zoom: number;
  onZoomChange: (zoom: number) => void;
}

export default function ImageViewer({ url, zoom, onZoomChange }: ImageViewerProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [dragging, setDragging] = useState(false);
  const [position, setPosition] = useState({ x: 0, y: 0 });
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 });
  const [error, setError] = useState(false);

  const handleWheel = useCallback((e: React.WheelEvent) => {
    e.preventDefault();
    const delta = e.deltaY > 0 ? -0.1 : 0.1;
    const newZoom = Math.max(0.25, Math.min(5, zoom + delta));
    onZoomChange(newZoom);
  }, [zoom, onZoomChange]);

  const handleMouseDown = useCallback((e: React.MouseEvent) => {
    if (zoom > 1) {
      setDragging(true);
      setDragStart({ x: e.clientX - position.x, y: e.clientY - position.y });
    }
  }, [zoom, position]);

  const handleMouseMove = useCallback((e: React.MouseEvent) => {
    if (dragging) {
      setPosition({
        x: e.clientX - dragStart.x,
        y: e.clientY - dragStart.y,
      });
    }
  }, [dragging, dragStart]);

  const handleMouseUp = useCallback(() => {
    setDragging(false);
  }, []);

  if (error) {
    return (
      <div style={{ padding: 40, textAlign: 'center', color: '#ef4444' }}>
        Failed to load image
      </div>
    );
  }

  return (
    <div
      ref={containerRef}
      style={{
        width: '100%',
        height: '100%',
        overflow: 'hidden',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        cursor: zoom > 1 ? (dragging ? 'grabbing' : 'grab') : 'default',
      }}
      onWheel={handleWheel}
      onMouseDown={handleMouseDown}
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
      onMouseLeave={handleMouseUp}
    >
      <img
        src={url}
        alt="Preview"
        style={{
          transform: `scale(${zoom}) translate(${position.x / zoom}px, ${position.y / zoom}px)`,
          transition: dragging ? 'none' : 'transform 0.1s ease',
          maxWidth: '100%',
          maxHeight: '100%',
          objectFit: 'contain',
          userSelect: 'none',
          pointerEvents: 'none',
        }}
        onError={() => setError(true)}
        draggable={false}
      />
    </div>
  );
}
