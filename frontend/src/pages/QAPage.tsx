import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { QAPanel } from '../components/qa/QAPanel';
import { ConversationList } from '../components/qa/ConversationList';

export default function QAPage() {
  const { workspaceId } = useParams<{ workspaceId: string }>();
  const [activeConversationId, setActiveConversationId] = useState<string | undefined>();
  const [panelKey, setPanelKey] = useState(0);

  if (!workspaceId) {
    return <div>No workspace selected</div>;
  }

  const handleNewConversation = () => {
    setActiveConversationId(undefined);
    setPanelKey(prev => prev + 1);
  };

  const handleSelectConversation = (conversationId: string) => {
    setActiveConversationId(conversationId);
    setPanelKey(prev => prev + 1);
  };

  return (
    <div style={{ height: 'calc(100vh - 64px)', display: 'flex' }}>
      <ConversationList
        workspaceId={workspaceId}
        activeConversationId={activeConversationId}
        onSelectConversation={handleSelectConversation}
        onNewConversation={handleNewConversation}
      />
      <div style={{ flex: 1, padding: '16px' }}>
        <QAPanel key={panelKey} workspaceId={workspaceId} />
      </div>
    </div>
  );
}
