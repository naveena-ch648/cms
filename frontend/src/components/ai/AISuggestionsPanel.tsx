import React, { useEffect, useState, useCallback } from 'react';
import { aiApi, type AISuggestions } from '../../api/ai';

interface AISuggestionsPanelProps {
  fileId: string;
  onTagsAccepted?: () => void;
}

const AISuggestionsPanel: React.FC<AISuggestionsPanelProps> = ({ fileId, onTagsAccepted }) => {
  const [suggestions, setSuggestions] = useState<AISuggestions | null>(null);
  const [loading, setLoading] = useState(true);
  const [regenerating, setRegenerating] = useState(false);
  const [selectedTags, setSelectedTags] = useState<Set<string>>(new Set());
  const [rejectedTags, setRejectedTags] = useState<Set<string>>(new Set());

  const fetchSuggestions = useCallback(() => {
    setLoading(true);
    aiApi.getSuggestions(fileId)
      .then(res => {
        setSuggestions(res.data.data);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  }, [fileId]);

  useEffect(() => {
    fetchSuggestions();
  }, [fetchSuggestions]);

  const handleAcceptTags = () => {
    const accepted = Array.from(selectedTags);
    const rejected = Array.from(rejectedTags);
    aiApi.acceptTags(fileId, accepted, rejected)
      .then(() => {
        onTagsAccepted?.();
        fetchSuggestions();
      })
      .catch(() => {});
  };

  const handleAcceptClassification = (category: string) => {
    aiApi.acceptClassification(fileId, category)
      .then(() => fetchSuggestions())
      .catch(() => {});
  };

  const handleRegenerate = (types?: string[]) => {
    setRegenerating(true);
    aiApi.regenerate(fileId, types)
      .then(() => {
        setTimeout(() => {
          fetchSuggestions();
          setRegenerating(false);
        }, 3000);
      })
      .catch(() => setRegenerating(false));
  };

  const handleApplyWorkflow = (workflowId: string) => {
    aiApi.applyWorkflow(fileId, workflowId)
      .then(() => fetchSuggestions())
      .catch(() => {});
  };

  const toggleTag = (tag: string) => {
    const newSelected = new Set(selectedTags);
    const newRejected = new Set(rejectedTags);
    if (newSelected.has(tag)) {
      newSelected.delete(tag);
      newRejected.add(tag);
    } else if (newRejected.has(tag)) {
      newRejected.delete(tag);
    } else {
      newSelected.add(tag);
    }
    setSelectedTags(newSelected);
    setRejectedTags(newRejected);
  };

  if (loading) {
    return (
      <div style={{ padding: 10, fontSize: 13, color: '#666' }}>
        <strong>AI Suggestions</strong>
        <div style={{ marginTop: 8 }}>Loading AI analysis...</div>
      </div>
    );
  }

  if (!suggestions || suggestions.processingStatus === 'PENDING') {
    return (
      <div style={{ padding: 10, background: '#f8f9fa', borderRadius: 6, fontSize: 13 }}>
        <strong>AI Suggestions</strong>
        <div style={{ marginTop: 8, color: '#666' }}>⏳ AI analysis pending...</div>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <strong style={{ fontSize: 13 }}>AI Suggestions</strong>
        <button
          onClick={() => handleRegenerate()}
          disabled={regenerating}
          style={{ fontSize: 11, padding: '2px 8px', cursor: 'pointer', border: '1px solid #ddd', borderRadius: 4, background: '#fff' }}
        >
          {regenerating ? '⟳ Regenerating...' : '🔄 Regenerate'}
        </button>
      </div>

      {/* Tags Section */}
      {suggestions.tags && suggestions.tags.status === 'COMPLETED' && suggestions.tags.suggestions.length > 0 && (
        <div style={{ padding: 10, background: '#f0f7ff', borderRadius: 6, fontSize: 12 }}>
          <div style={{ fontWeight: 600, marginBottom: 6 }}>Suggested Tags</div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
            {suggestions.tags.suggestions.map(tag => {
              const isAccepted = selectedTags.has(tag);
              const isRejected = rejectedTags.has(tag);
              const confidence = suggestions.tags!.confidence[tag];
              return (
                <span
                  key={tag}
                  onClick={() => toggleTag(tag)}
                  style={{
                    padding: '2px 8px',
                    borderRadius: 12,
                    fontSize: 11,
                    cursor: 'pointer',
                    border: '1px solid',
                    borderColor: isAccepted ? '#28a745' : isRejected ? '#dc3545' : '#6c757d',
                    background: isAccepted ? '#e6f4ea' : isRejected ? '#f8d7da' : '#fff',
                    color: isAccepted ? '#1e7e34' : isRejected ? '#721c24' : '#333',
                  }}
                  title={`Confidence: ${confidence?.toFixed(0) ?? '?'}%`}
                >
                  {isAccepted ? '✓ ' : isRejected ? '✗ ' : ''}{tag}
                </span>
              );
            })}
          </div>
          {(selectedTags.size > 0 || rejectedTags.size > 0) && (
            <button
              onClick={handleAcceptTags}
              style={{ marginTop: 8, fontSize: 11, padding: '4px 12px', cursor: 'pointer', background: '#007bff', color: '#fff', border: 'none', borderRadius: 4 }}
            >
              Apply Selection
            </button>
          )}
        </div>
      )}

      {/* Classification Section */}
      {suggestions.classification && suggestions.classification.status === 'COMPLETED' && suggestions.classification.category && (
        <div style={{ padding: 10, background: '#f0fff4', borderRadius: 6, fontSize: 12 }}>
          <div style={{ fontWeight: 600, marginBottom: 6 }}>Classification</div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ padding: '2px 10px', borderRadius: 12, background: '#d4edda', color: '#155724', fontWeight: 600, fontSize: 11 }}>
              {suggestions.classification.category}
            </span>
            <span style={{ color: '#666', fontSize: 11 }}>
              {suggestions.classification.confidence?.toFixed(0)}% confidence
            </span>
            <button
              onClick={() => handleAcceptClassification(suggestions.classification!.category)}
              style={{ fontSize: 10, padding: '2px 6px', cursor: 'pointer', border: '1px solid #28a745', borderRadius: 3, background: '#fff', color: '#28a745' }}
            >
              Accept
            </button>
          </div>
          {suggestions.classification.alternatives && suggestions.classification.alternatives.length > 0 && (
            <div style={{ marginTop: 6, color: '#666', fontSize: 11 }}>
              Alternatives: {suggestions.classification.alternatives.map(a => `${a.category} (${a.confidence?.toFixed(0)}%)`).join(', ')}
            </div>
          )}
        </div>
      )}

      {/* Summary Section */}
      {suggestions.summary && suggestions.summary.status === 'COMPLETED' && suggestions.summary.text && (
        <div style={{ padding: 10, background: '#fff8f0', borderRadius: 6, fontSize: 12 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
            <span style={{ fontWeight: 600 }}>Summary</span>
            <span style={{ fontSize: 10, color: '#666' }}>{suggestions.summary.wordCount} words</span>
          </div>
          <div style={{ color: '#333', lineHeight: 1.5, maxHeight: 120, overflowY: 'auto' }}>
            {suggestions.summary.text}
          </div>
          {suggestions.summary.keyTopics && suggestions.summary.keyTopics.length > 0 && (
            <div style={{ marginTop: 6, fontSize: 11, color: '#666' }}>
              Topics: {suggestions.summary.keyTopics.join(', ')}
            </div>
          )}
          <button
            onClick={() => handleRegenerate(['SUMMARIZE'])}
            disabled={regenerating}
            style={{ marginTop: 6, fontSize: 10, padding: '2px 8px', cursor: 'pointer', border: '1px solid #ddd', borderRadius: 3, background: '#fff' }}
          >
            Regenerate Summary
          </button>
        </div>
      )}

      {/* Workflow Recommendation Section */}
      {suggestions.workflowRecommendation && suggestions.workflowRecommendation.status === 'COMPLETED' && suggestions.workflowRecommendation.recommendedWorkflow && (
        <div style={{ padding: 10, background: '#f8f0ff', borderRadius: 6, fontSize: 12 }}>
          <div style={{ fontWeight: 600, marginBottom: 6 }}>Workflow Recommendation</div>
          <div style={{ color: '#333', marginBottom: 6 }}>
            💡 {suggestions.workflowRecommendation.reason}
          </div>
          <div style={{ display: 'flex', gap: 6 }}>
            <button
              onClick={() => handleApplyWorkflow(suggestions.workflowRecommendation!.workflowId)}
              style={{ fontSize: 11, padding: '4px 10px', cursor: 'pointer', background: '#6f42c1', color: '#fff', border: 'none', borderRadius: 4 }}
            >
              Apply: {suggestions.workflowRecommendation.recommendedWorkflow}
            </button>
            <button
              style={{ fontSize: 11, padding: '4px 10px', cursor: 'pointer', border: '1px solid #ddd', borderRadius: 4, background: '#fff' }}
            >
              Dismiss
            </button>
          </div>
        </div>
      )}

      {/* Processing status for pending items */}
      {suggestions.processingStatus === 'PROCESSING' && (
        <div style={{ fontSize: 11, color: '#856404', padding: '4px 8px', background: '#fff3cd', borderRadius: 4 }}>
          ⟳ Some analyses are still processing...
        </div>
      )}
    </div>
  );
};

export default AISuggestionsPanel;
