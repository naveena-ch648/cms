import { useState, useRef, useEffect, useCallback } from 'react';
import type { WorkspaceMember } from '../../types/collaboration';

interface MentionInputProps {
  value: string;
  onChange: (value: string) => void;
  onSubmit: () => void;
  members: WorkspaceMember[];
  placeholder?: string;
}

export default function MentionInput({ value, onChange, onSubmit, members, placeholder }: MentionInputProps) {
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [suggestionFilter, setSuggestionFilter] = useState('');
  const [cursorPosition, setCursorPosition] = useState(0);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const filteredMembers = members.filter(m => {
    const name = `${m.firstName} ${m.lastName}`.toLowerCase();
    return name.includes(suggestionFilter.toLowerCase()) || m.email.toLowerCase().includes(suggestionFilter.toLowerCase());
  }).slice(0, 5);

  const handleChange = useCallback((e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const newValue = e.target.value;
    const pos = e.target.selectionStart;
    onChange(newValue);
    setCursorPosition(pos);

    // Check if we're in a mention context
    const textBeforeCursor = newValue.substring(0, pos);
    const mentionMatch = textBeforeCursor.match(/@(\w*)$/);
    if (mentionMatch) {
      setSuggestionFilter(mentionMatch[1] || '');
      setShowSuggestions(true);
      setSelectedIndex(0);
    } else {
      setShowSuggestions(false);
    }
  }, [onChange]);

  const insertMention = useCallback((member: WorkspaceMember) => {
    const textBeforeCursor = value.substring(0, cursorPosition);
    const textAfterCursor = value.substring(cursorPosition);
    const mentionMatch = textBeforeCursor.match(/@(\w*)$/);
    if (mentionMatch) {
      const beforeMention = textBeforeCursor.substring(0, textBeforeCursor.length - mentionMatch[0].length);
      const mentionText = `@[${member.id}](${member.firstName} ${member.lastName})`;
      const newValue = beforeMention + mentionText + ' ' + textAfterCursor;
      onChange(newValue);
      setShowSuggestions(false);
      // Focus back on textarea
      setTimeout(() => textareaRef.current?.focus(), 0);
    }
  }, [value, cursorPosition, onChange]);

  const handleKeyDown = useCallback((e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (showSuggestions && filteredMembers.length > 0) {
      if (e.key === 'ArrowDown') {
        e.preventDefault();
        setSelectedIndex(i => Math.min(i + 1, filteredMembers.length - 1));
      } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        setSelectedIndex(i => Math.max(i - 1, 0));
      } else if (e.key === 'Enter' || e.key === 'Tab') {
        e.preventDefault();
        if (filteredMembers[selectedIndex]) insertMention(filteredMembers[selectedIndex]);
      } else if (e.key === 'Escape') {
        setShowSuggestions(false);
      }
    } else if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
      e.preventDefault();
      onSubmit();
    }
  }, [showSuggestions, filteredMembers, selectedIndex, insertMention, onSubmit]);

  useEffect(() => {
    setSelectedIndex(0);
  }, [suggestionFilter]);

  return (
    <div style={{ position: 'relative' }}>
      <textarea
        ref={textareaRef}
        value={value}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        placeholder={placeholder || 'Write a comment... Use @ to mention'}
        style={{
          width: '100%',
          minHeight: '60px',
          padding: '8px',
          border: '1px solid #ddd',
          borderRadius: '4px',
          resize: 'vertical',
          fontFamily: 'inherit',
          fontSize: '13px',
        }}
      />
      {showSuggestions && filteredMembers.length > 0 && (
        <div style={{
          position: 'absolute',
          bottom: '100%',
          left: 0,
          right: 0,
          background: '#fff',
          border: '1px solid #ddd',
          borderRadius: '4px',
          boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
          zIndex: 100,
          maxHeight: '200px',
          overflow: 'auto',
        }}>
          {filteredMembers.map((member, idx) => (
            <div
              key={member.id}
              onClick={() => insertMention(member)}
              style={{
                padding: '8px 12px',
                cursor: 'pointer',
                background: idx === selectedIndex ? '#f0f0f0' : '#fff',
                borderBottom: idx < filteredMembers.length - 1 ? '1px solid #eee' : 'none',
              }}
            >
              <div style={{ fontWeight: 500, fontSize: '13px' }}>
                {member.firstName} {member.lastName}
              </div>
              <div style={{ fontSize: '11px', color: '#666' }}>{member.email}</div>
            </div>
          ))}
        </div>
      )}
      <div style={{ fontSize: '11px', color: '#888', marginTop: '4px' }}>
        Ctrl+Enter to submit • @ to mention
      </div>
    </div>
  );
}
