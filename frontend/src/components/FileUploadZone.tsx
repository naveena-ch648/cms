import React, { useCallback, useState, useRef } from 'react';

interface FileUploadZoneProps {
  onFilesSelected: (files: File[]) => void;
  disabled?: boolean;
}

const FileUploadZone: React.FC<FileUploadZoneProps> = ({ onFilesSelected, disabled }) => {
  const [isDragOver, setIsDragOver] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (!disabled) setIsDragOver(true);
  }, [disabled]);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);
  }, []);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);
    if (disabled) return;

    const files = Array.from(e.dataTransfer.files);
    if (files.length > 0) {
      onFilesSelected(files);
    }
  }, [disabled, onFilesSelected]);

  const handleFileInput = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files ? Array.from(e.target.files) : [];
    if (files.length > 0) {
      onFilesSelected(files);
    }
    // Reset input so same file can be selected again
    e.target.value = '';
  }, [onFilesSelected]);

  const handleClick = () => {
    if (!disabled) {
      fileInputRef.current?.click();
    }
  };

  return (
    <div
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
      onClick={handleClick}
      style={{
        border: `2px dashed ${isDragOver ? '#2196f3' : '#ccc'}`,
        borderRadius: '8px',
        padding: '32px',
        textAlign: 'center',
        cursor: disabled ? 'not-allowed' : 'pointer',
        backgroundColor: isDragOver ? '#e3f2fd' : '#fafafa',
        transition: 'all 0.2s ease',
        opacity: disabled ? 0.5 : 1,
      }}
    >
      <input
        ref={fileInputRef}
        type="file"
        multiple
        onChange={handleFileInput}
        style={{ display: 'none' }}
      />
      <p style={{ margin: 0, fontSize: '16px', color: '#666' }}>
        {isDragOver ? 'Drop files here' : 'Drag & drop files here or click to browse'}
      </p>
      <p style={{ margin: '8px 0 0', fontSize: '12px', color: '#999' }}>
        Supports multiple files
      </p>
    </div>
  );
};

export default FileUploadZone;
