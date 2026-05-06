import { useEffect, useRef, useState } from 'react';
import * as pdfjsLib from 'pdfjs-dist';

pdfjsLib.GlobalWorkerOptions.workerSrc = `https://cdnjs.cloudflare.com/ajax/libs/pdf.js/${pdfjsLib.version}/pdf.worker.min.mjs`;

interface PdfViewerProps {
  url: string;
  zoom: number;
  currentPage: number;
  onPageCountChange: (count: number) => void;
}

export default function PdfViewer({ url, zoom, currentPage, onPageCountChange }: PdfViewerProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [pdfDoc, setPdfDoc] = useState<pdfjsLib.PDFDocumentProxy | null>(null);
  const [rendering, setRendering] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    const loadPdf = async () => {
      try {
        const doc = await pdfjsLib.getDocument(url).promise;
        if (!cancelled) {
          setPdfDoc(doc);
          onPageCountChange(doc.numPages);
        }
      } catch (err) {
        if (!cancelled) {
          setError('Failed to load PDF document');
          console.error('PDF load error:', err);
        }
      }
    };

    loadPdf();
    return () => { cancelled = true; };
  }, [url]);

  useEffect(() => {
    if (!pdfDoc || !canvasRef.current) return;

    let cancelled = false;
    setRendering(true);

    const renderPage = async () => {
      try {
        const page = await pdfDoc.getPage(currentPage);
        const viewport = page.getViewport({ scale: zoom });

        const canvas = canvasRef.current!;
        const context = canvas.getContext('2d')!;
        canvas.height = viewport.height;
        canvas.width = viewport.width;

        const renderContext = {
          canvasContext: context,
          viewport: viewport,
        };

        await page.render(renderContext).promise;
        if (!cancelled) {
          setRendering(false);
        }
      } catch (err) {
        if (!cancelled) {
          setRendering(false);
          console.error('PDF render error:', err);
        }
      }
    };

    renderPage();
    return () => { cancelled = true; };
  }, [pdfDoc, currentPage, zoom]);

  if (error) {
    return <div style={{ padding: 40, textAlign: 'center', color: '#ef4444' }}>{error}</div>;
  }

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'flex-start', overflow: 'auto', height: '100%' }}>
      <canvas ref={canvasRef} style={{ display: 'block' }} />
      {rendering && (
        <div style={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)' }}>
          Loading page...
        </div>
      )}
    </div>
  );
}
