import React, { useState, useRef, useEffect } from 'react';
import { qaApi, type AskResponse, type Citation } from '../../api/qa';
import { MessageBubble } from './MessageBubble';
import { CitationPanel } from './CitationPanel';

interface Message {
  id: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  citations?: Citation[] | null;
}

interface QAPanelProps {
  workspaceId: string;
  onOpenDocument?: (documentId: string, pageNumber: number) => void;
}

export const QAPanel: React.FC<QAPanelProps> = ({ workspaceId, onOpenDocument }) => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [conversationId, setConversationId] = useState<string | undefined>();
  const [error, setError] = useState<string | null>(null);
  const [selectedCitation, setSelectedCitation] = useState<Citation | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isLoading]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const question = input.trim();
    if (!question || isLoading) return;

    setInput('');
    setError(null);

    const userMessage: Message = {
      id: `user-${Date.now()}`,
      role: 'USER',
      content: question,
    };
    setMessages(prev => [...prev, userMessage]);
    setIsLoading(true);

    try {
      const response: AskResponse = await qaApi.ask({
        question,
        workspaceId,
        conversationId,
      });

      setConversationId(response.conversationId);

      const assistantMessage: Message = {
        id: response.messageId,
        role: 'ASSISTANT',
        content: response.answer,
        citations: response.citations,
      };
      setMessages(prev => [...prev, assistantMessage]);
    } catch (err: unknown) {
      let errorMsg = 'Failed to get response';
      if (err && typeof err === 'object' && 'response' in err) {
        const axiosErr = err as { response?: { status?: number; data?: { message?: string } } };
        const status = axiosErr.response?.status;
        const serverMsg = axiosErr.response?.data?.message;
        if (status === 429) {
          errorMsg = 'Rate limit exceeded. Please wait a moment before asking again.';
        } else if (status === 503 || (serverMsg && /qdrant|vector|unavailable/i.test(serverMsg))) {
          errorMsg = 'AI search service is temporarily unavailable. Please try again later.';
        } else if (serverMsg && /llm|openai|model/i.test(serverMsg)) {
          errorMsg = 'AI language model is temporarily unavailable. Please try again later.';
        } else if (serverMsg) {
          errorMsg = serverMsg;
        }
      } else if (err instanceof Error) {
        errorMsg = err.message;
      }
      setError(errorMsg);
    } finally {
      setIsLoading(false);
    }
  };

  const handleNewConversation = () => {
    setMessages([]);
    setConversationId(undefined);
    setError(null);
  };

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      height: '100%',
      backgroundColor: '#fff',
      borderRadius: '8px',
      border: '1px solid #e5e7eb',
    }}>
      {/* Header */}
      <div style={{
        padding: '12px 16px',
        borderBottom: '1px solid #e5e7eb',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
      }}>
        <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 600 }}>Document Q&A</h3>
        {messages.length > 0 && (
          <button
            onClick={handleNewConversation}
            style={{
              padding: '4px 12px',
              fontSize: '12px',
              backgroundColor: '#f3f4f6',
              border: '1px solid #d1d5db',
              borderRadius: '4px',
              cursor: 'pointer',
            }}
          >
            New Chat
          </button>
        )}
      </div>

      {/* Messages */}
      <div style={{
        flex: 1,
        overflowY: 'auto',
        padding: '16px',
      }}>
        {messages.length === 0 && !isLoading && (
          <div style={{
            textAlign: 'center',
            color: '#9ca3af',
            padding: '40px 20px',
          }}>
            <p style={{ fontSize: '16px', marginBottom: '8px' }}>Ask questions about your documents</p>
            <p style={{ fontSize: '13px' }}>Your uploaded files will be searched to find relevant answers.</p>
          </div>
        )}
        {messages.map(msg => (
          <MessageBubble
            key={msg.id}
            role={msg.role}
            content={msg.content}
            citations={msg.citations}
            onCitationClick={setSelectedCitation}
          />
        ))}
        {isLoading && (
          <MessageBubble role="ASSISTANT" content="" isLoading />
        )}
        {error && (
          <div style={{
            padding: '8px 12px',
            backgroundColor: '#fef2f2',
            color: '#dc2626',
            borderRadius: '6px',
            fontSize: '13px',
            marginTop: '8px',
          }}>
            {error}
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <form onSubmit={handleSubmit} style={{
        padding: '12px 16px',
        borderTop: '1px solid #e5e7eb',
        display: 'flex',
        gap: '8px',
      }}>
        <input
          type="text"
          value={input}
          onChange={e => setInput(e.target.value)}
          placeholder="Ask a question about your documents..."
          disabled={isLoading}
          style={{
            flex: 1,
            padding: '10px 14px',
            border: '1px solid #d1d5db',
            borderRadius: '8px',
            fontSize: '14px',
            outline: 'none',
          }}
        />
        <button
          type="submit"
          disabled={isLoading || !input.trim()}
          style={{
            padding: '10px 20px',
            backgroundColor: isLoading || !input.trim() ? '#93c5fd' : '#2563eb',
            color: '#fff',
            border: 'none',
            borderRadius: '8px',
            fontSize: '14px',
            fontWeight: 500,
            cursor: isLoading || !input.trim() ? 'not-allowed' : 'pointer',
          }}
        >
          Send
        </button>
      </form>

      {selectedCitation && (
        <CitationPanel
          citation={selectedCitation}
          onClose={() => setSelectedCitation(null)}
          onOpenDocument={(docId, page) => {
            setSelectedCitation(null);
            onOpenDocument?.(docId, page);
          }}
        />
      )}
    </div>
  );
};
