import { useState, FormEvent } from "react";
import { useAuth } from "../contexts/AuthContext";
import { useNavigate } from "react-router-dom";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError("");
    setIsSubmitting(true);
    try {
      await login(email, password);
      navigate("/");
    } catch (err: any) {
      setError(err.response?.data?.error?.message || "Login failed");
    } finally {
      setIsSubmitting(false);
    }
  };

  const pageStyle: React.CSSProperties = {
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
    minHeight: "100vh",
    background: "#f5f7fb",
    padding: 16,
  };

  const cardStyle: React.CSSProperties = {
    width: 380,
    padding: 28,
    background: "#ffffff",
    borderRadius: 8,
    boxShadow: "0 6px 18px rgba(31,41,55,0.08)",
    color: "#1f2937",
  };

  const labelStyle: React.CSSProperties = {
    display: "block",
    marginBottom: 6,
    color: "#374151",
    fontSize: 14,
  };
  const inputStyle: React.CSSProperties = {
    display: "block",
    width: "100%",
    padding: 10,
    border: "1px solid #e5e7eb",
    borderRadius: 6,
    boxSizing: "border-box",
  };
  const errorStyle: React.CSSProperties = {
    color: "#b00020",
    marginBottom: 12,
  };
  const buttonStyle: React.CSSProperties = {
    width: "100%",
    padding: 10,
    background: "#2563eb",
    color: "#fff",
    border: "none",
    borderRadius: 6,
    cursor: "pointer",
  };

  return (
    <div style={pageStyle}>
      <form onSubmit={handleSubmit} style={cardStyle}>
        <div style={{ textAlign: "center", marginBottom: 16 }}>
          <div style={{ fontSize: 28, fontWeight: 700, color: "#111827" }}>
            CMS
          </div>
          <div style={{ fontSize: 16, color: "#6b7280", marginTop: 4 }}>
            Sign in to your account
          </div>
        </div>
        {error && <div style={errorStyle}>{error}</div>}
        <div style={{ marginBottom: 12 }}>
          <label htmlFor="email" style={labelStyle}>
            Email
          </label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            style={inputStyle}
          />
        </div>
        <div style={{ marginBottom: 14 }}>
          <label htmlFor="password" style={labelStyle}>
            Password
          </label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            style={inputStyle}
          />
        </div>
        <button type="submit" disabled={isSubmitting} style={buttonStyle}>
          {isSubmitting ? "Signing in..." : "Sign In"}
        </button>
      </form>
    </div>
  );
}
