import { useRef, useState } from 'react';

interface VideoPlayerProps {
  url: string;
}

export default function VideoPlayer({ url }: VideoPlayerProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [error, setError] = useState(false);

  if (error) {
    return (
      <div style={{ padding: 40, textAlign: 'center', color: '#ef4444' }}>
        Failed to load video. <a href={url} download style={{ color: '#3b82f6' }}>Download instead</a>
      </div>
    );
  }

  return (
    <div style={{
      width: '100%',
      height: '100%',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: '#000',
    }}>
      <video
        ref={videoRef}
        src={url}
        controls
        autoPlay={false}
        style={{
          maxWidth: '100%',
          maxHeight: '100%',
        }}
        onError={() => setError(true)}
      >
        Your browser does not support the video element.
      </video>
    </div>
  );
}
