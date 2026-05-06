import { useState, useEffect, useCallback } from 'react';
import { tasksApi } from '../../api/tasks';
import type { TaskItem, CreateTaskRequest } from '../../types/collaboration';

interface TaskPanelProps {
  fileId: string;
  currentUserId: string;
}

export default function TaskPanel({ fileId, currentUserId }: TaskPanelProps) {
  const [tasks, setTasks] = useState<TaskItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loadTasks = useCallback(async () => {
    try {
      setLoading(true);
      const res = await tasksApi.getFileTasks(fileId);
      setTasks(res.data.data);
    } catch {
      // silent
    } finally {
      setLoading(false);
    }
  }, [fileId]);

  useEffect(() => {
    loadTasks();
  }, [loadTasks]);

  const handleCreate = async () => {
    if (!title.trim() || submitting) return;
    setSubmitting(true);
    try {
      const req: CreateTaskRequest = { title: title.trim(), description: description.trim() || undefined, dueDate: dueDate || undefined };
      await tasksApi.createTask(fileId, req);
      setTitle('');
      setDescription('');
      setDueDate('');
      setShowForm(false);
      await loadTasks();
    } catch {
      // silent
    } finally {
      setSubmitting(false);
    }
  };

  const handleToggleStatus = async (task: TaskItem) => {
    try {
      const newStatus = task.status === 'OPEN' ? 'DONE' : 'OPEN';
      await tasksApi.updateTask(task.id, { status: newStatus });
      await loadTasks();
    } catch {
      // silent
    }
  };

  const handleDelete = async (taskId: string) => {
    if (!confirm('Delete this task?')) return;
    try {
      await tasksApi.deleteTask(taskId);
      await loadTasks();
    } catch {
      // silent
    }
  };

  if (loading) {
    return <div style={{ padding: 16, color: '#888', textAlign: 'center' }}>Loading tasks...</div>;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div style={{ flex: 1, overflowY: 'auto', padding: '12px' }}>
        {tasks.length === 0 && !showForm ? (
          <div style={{ textAlign: 'center', padding: 24, color: '#888' }}>
            <p style={{ margin: 0 }}>No tasks yet</p>
            <p style={{ margin: '4px 0 0', fontSize: 13 }}>Create a task to track work on this file</p>
          </div>
        ) : (
          tasks.map(task => (
            <div
              key={task.id}
              style={{
                padding: '10px 12px',
                borderBottom: '1px solid #f0f0f0',
                display: 'flex',
                alignItems: 'flex-start',
                gap: 8,
              }}
            >
              <input
                type="checkbox"
                checked={task.status === 'DONE'}
                onChange={() => handleToggleStatus(task)}
                style={{ marginTop: 3, cursor: 'pointer' }}
              />
              <div style={{ flex: 1 }}>
                <div style={{
                  fontSize: 13,
                  fontWeight: 500,
                  textDecoration: task.status === 'DONE' ? 'line-through' : 'none',
                  color: task.status === 'DONE' ? '#888' : '#333',
                }}>
                  {task.title}
                </div>
                {task.description && (
                  <div style={{ fontSize: 12, color: '#666', marginTop: 2 }}>{task.description}</div>
                )}
                <div style={{ fontSize: 11, color: '#999', marginTop: 4, display: 'flex', gap: 8 }}>
                  {task.dueDate && (
                    <span style={{ color: task.overdue ? '#e53e3e' : '#888' }}>
                      Due: {task.dueDate}
                    </span>
                  )}
                  {task.assignee && (
                    <span>→ {task.assignee.name}</span>
                  )}
                </div>
              </div>
              {task.creator.id === currentUserId && (
                <button
                  onClick={() => handleDelete(task.id)}
                  style={{ border: 'none', background: 'none', cursor: 'pointer', color: '#999', fontSize: 14 }}
                  title="Delete task"
                >
                  ✕
                </button>
              )}
            </div>
          ))
        )}
      </div>

      <div style={{ borderTop: '1px solid #e8e8e8', padding: 12 }}>
        {showForm ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Task title"
              style={{ padding: 8, border: '1px solid #ddd', borderRadius: 4, fontSize: 13 }}
              onKeyDown={(e) => e.key === 'Enter' && handleCreate()}
            />
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Description (optional)"
              style={{ padding: 8, border: '1px solid #ddd', borderRadius: 4, fontSize: 13, resize: 'none', minHeight: 40 }}
            />
            <input
              type="date"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
              style={{ padding: 8, border: '1px solid #ddd', borderRadius: 4, fontSize: 13 }}
            />
            <div style={{ display: 'flex', gap: 8 }}>
              <button
                onClick={handleCreate}
                disabled={!title.trim() || submitting}
                style={{
                  flex: 1, padding: '8px', border: 'none', borderRadius: 4, cursor: 'pointer',
                  backgroundColor: title.trim() ? '#1976d2' : '#ccc', color: '#fff', fontSize: 13,
                }}
              >
                {submitting ? '...' : 'Create Task'}
              </button>
              <button
                onClick={() => { setShowForm(false); setTitle(''); setDescription(''); setDueDate(''); }}
                style={{ padding: '8px 12px', border: '1px solid #ddd', borderRadius: 4, cursor: 'pointer', background: '#fff', fontSize: 13 }}
              >
                Cancel
              </button>
            </div>
          </div>
        ) : (
          <button
            onClick={() => setShowForm(true)}
            style={{
              width: '100%', padding: '8px', border: '1px dashed #ccc', borderRadius: 4,
              cursor: 'pointer', background: '#fafafa', fontSize: 13, color: '#666',
            }}
          >
            + Add Task
          </button>
        )}
      </div>
    </div>
  );
}
