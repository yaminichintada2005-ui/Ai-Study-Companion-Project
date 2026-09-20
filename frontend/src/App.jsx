import React, { useEffect, useMemo, useState } from "react";

const API_BASE = "https://ai-study-companion-project.onrender.com/api";

/* =============================================================
   AI STUDY COMPANION - COMPLETE FRONTEND
   Flow:
   Login -> Space -> Project -> PDF/TXT -> Summary/Explain/Quiz
   -> AI Tutor with the selected material as context
   ============================================================= */

function App() {
  const [page, setPage] = useState(getInitialPage());
  const [token, setToken] = useState(() => localStorage.getItem("token"));
  const [email, setEmail] = useState(() => localStorage.getItem("email") || "");

  const [spaces, setSpaces] = useState([]);
  const [selectedSpace, setSelectedSpace] = useState(() => readStored("selectedSpace"));
  const [projects, setProjects] = useState([]);
  const [selectedProject, setSelectedProject] = useState(() => readStored("selectedProject"));
  const [materials, setMaterials] = useState([]);

  const [loading, setLoading] = useState(false);
  const [globalError, setGlobalError] = useState("");
  const [globalMessage, setGlobalMessage] = useState("");

  const [aiResult, setAiResult] = useState("");
  const [aiResultTitle, setAiResultTitle] = useState("");

  function getInitialPage() {
    const value = window.location.pathname.replace(/^\//, "");
    return value || "dashboard";
  }

  function readStored(key) {
    try {
      const value = localStorage.getItem(key);
      return value ? JSON.parse(value) : null;
    } catch {
      return null;
    }
  }

  const clearMessages = () => {
    setGlobalError("");
    setGlobalMessage("");
  };

  const navigate = (nextPage) => {
    setPage(nextPage);
    window.history.pushState({}, "", `/${nextPage}`);
    clearMessages();
  };

  useEffect(() => {
    const onPopState = () => setPage(getInitialPage());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  const saveSelection = (key, value) => {
    if (value == null) localStorage.removeItem(key);
    else localStorage.setItem(key, JSON.stringify(value));
  };

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("email");
    localStorage.removeItem("userId");
    localStorage.removeItem("selectedSpace");
    localStorage.removeItem("selectedProject");
    localStorage.removeItem("selectedProjectId");
    setToken(null);
    setEmail("");
    setSpaces([]);
    setProjects([]);
    setMaterials([]);
    setSelectedSpace(null);
    setSelectedProject(null);
    navigate("login");
  };

  const authenticatedFetch = async (url, options = {}) => {
    const currentToken = localStorage.getItem("token");
    const isFormData = options.body instanceof FormData;

    const headers = {
      ...(isFormData ? {} : { "Content-Type": "application/json" }),
      ...(options.headers || {}),
      ...(currentToken ? { Authorization: `Bearer ${currentToken}` } : {}),
    };

    const response = await fetch(url, { ...options, headers });

    if (response.status === 401) {
      console.error("Authentication failed (401):", url);
      localStorage.removeItem("token");
      localStorage.removeItem("email");
      localStorage.removeItem("userId");
      setToken(null);
      setEmail("");
      setGlobalError("Your login session has expired. Please login again.");
      window.history.replaceState({}, "", "/login");
      setPage("login");
      throw new Error("Authentication failed. Please login again.");
    }

    if (response.status === 403) {
      // IMPORTANT: read the body. Spring puts the real reason in here.
      let detail = "";
      try {
        const raw = await response.clone().text();
        try {
          const parsed = JSON.parse(raw);
          detail = parsed.message || parsed.error || parsed.detail || raw;
        } catch {
          detail = raw;
        }
      } catch {
        detail = "";
      }

      console.error("Access denied (403):", url, detail);
      throw new Error(
        detail
          ? `Access denied (403): ${detail}`
          : "Access denied (403) with an empty body. Look at the backend terminal for the stack trace."
      );
    }

    return response;
  };

  const readError = async (response, fallback) => {
    let text = "";
    try {
      text = await response.text();
    } catch {
      // ignore
    }
    return text || `${fallback} (${response.status})`;
  };

  /* =========================================================
     LOAD SPACES
     ========================================================= */
  const loadSpaces = async () => {
    if (!localStorage.getItem("token")) return;

    try {
      setLoading(true);
      const response = await authenticatedFetch(`${API_BASE}/spaces`, { method: "GET" });
      if (!response.ok) throw new Error(await readError(response, "Unable to load spaces"));
      const data = await response.json();
      const list = Array.isArray(data) ? data : [];
      setSpaces(list);

      if (selectedSpace) {
        const fresh = list.find((item) => String(item.id) === String(selectedSpace.id));
        if (fresh) {
          setSelectedSpace(fresh);
          saveSelection("selectedSpace", fresh);
        }
      }
    } catch (error) {
      console.error(error);
      if (!error.message.includes("Authentication failed")) {
        setGlobalError(error.message || "Unable to load spaces.");
      }
    } finally {
      setLoading(false);
    }
  };

  /* =========================================================
     CREATE SPACE
     ========================================================= */
  const createSpace = async (name, description) => {
    if (!name.trim()) {
      setGlobalError("Please enter a space name.");
      return null;
    }

    try {
      setLoading(true);
      clearMessages();

      const response = await authenticatedFetch(`${API_BASE}/spaces`, {
        method: "POST",
        body: JSON.stringify({
          name: name.trim(),
          description: description.trim(),
        }),
      });

      if (!response.ok) throw new Error(await readError(response, "Unable to create space"));

      const created = await response.json();
      setSpaces((current) => [...current, created]);
      setSelectedSpace(created);
      saveSelection("selectedSpace", created);
      setGlobalMessage("Study space created successfully.");
      return created;
    } catch (error) {
      console.error("Create space error:", error);
      if (!error.message.includes("Authentication failed")) setGlobalError(error.message);
      return null;
    } finally {
      setLoading(false);
    }
  };

  const deleteSpace = async (spaceId) => {
    if (!window.confirm("Delete this study space and its projects?")) return;

    try {
      setLoading(true);
      clearMessages();
      const response = await authenticatedFetch(`${API_BASE}/spaces/${spaceId}`, { method: "DELETE" });
      if (!response.ok) throw new Error(await readError(response, "Unable to delete space"));

      setSpaces((current) => current.filter((item) => String(item.id) !== String(spaceId)));
      if (String(selectedSpace?.id) === String(spaceId)) {
        setSelectedSpace(null);
        setProjects([]);
        setMaterials([]);
        setSelectedProject(null);
        saveSelection("selectedSpace", null);
        saveSelection("selectedProject", null);
      }
      setGlobalMessage("Study space deleted.");
    } catch (error) {
      console.error(error);
      if (!error.message.includes("Authentication failed")) setGlobalError(error.message);
    } finally {
      setLoading(false);
    }
  };

  /* =========================================================
     PROJECTS
     ========================================================= */
  const loadProjects = async (spaceId) => {
    if (!spaceId) return;
    try {
      setLoading(true);
      clearMessages();
      const response = await authenticatedFetch(`${API_BASE}/projects/space/${spaceId}`, { method: "GET" });
      if (!response.ok) throw new Error(await readError(response, "Unable to load projects"));
      const data = await response.json();
      const list = Array.isArray(data) ? data : [];
      setProjects(list);

      if (selectedProject) {
        const fresh = list.find((item) => String(item.id) === String(selectedProject.id));
        if (fresh) {
          setSelectedProject(fresh);
          saveSelection("selectedProject", fresh);
        }
      }
    } catch (error) {
      console.error(error);
      if (!error.message.includes("Authentication failed")) setGlobalError(error.message);
    } finally {
      setLoading(false);
    }
  };

  const createProject = async (spaceId, name, description, learningGoal) => {
    if (!spaceId) {
      setGlobalError("Please select a study space first.");
      return null;
    }
    if (!name.trim()) {
      setGlobalError("Please enter a project name.");
      return null;
    }

    try {
      setLoading(true);
      clearMessages();
      const response = await authenticatedFetch(`${API_BASE}/projects/space/${spaceId}`, {
        method: "POST",
        body: JSON.stringify({
          name: name.trim(),
          description: description.trim(),
          learningGoal: learningGoal.trim(),
        }),
      });

      if (!response.ok) throw new Error(await readError(response, "Unable to create project"));
      const created = await response.json();
      setProjects((current) => [...current, created]);
      setSelectedProject(created);
      saveSelection("selectedProject", created);
      localStorage.setItem("selectedProjectId", String(created.id));
      setGlobalMessage("Project created successfully.");
      return created;
    } catch (error) {
      console.error("Create project error:", error);
      if (!error.message.includes("Authentication failed")) setGlobalError(error.message);
      return null;
    } finally {
      setLoading(false);
    }
  };

  const deleteProject = async (projectId) => {
    if (!window.confirm("Delete this project and its materials?")) return;

    try {
      setLoading(true);
      clearMessages();
      const response = await authenticatedFetch(`${API_BASE}/projects/${projectId}`, { method: "DELETE" });
      if (!response.ok) throw new Error(await readError(response, "Unable to delete project"));

      setProjects((current) => current.filter((item) => String(item.id) !== String(projectId)));
      if (String(selectedProject?.id) === String(projectId)) {
        setSelectedProject(null);
        setMaterials([]);
        saveSelection("selectedProject", null);
        localStorage.removeItem("selectedProjectId");
      }
      setGlobalMessage("Project deleted.");
    } catch (error) {
      console.error(error);
      if (!error.message.includes("Authentication failed")) setGlobalError(error.message);
    } finally {
      setLoading(false);
    }
  };

  /* =========================================================
     MATERIALS
     ========================================================= */
  const loadMaterials = async (projectId) => {
    if (!projectId) return;
    try {
      setLoading(true);
      clearMessages();
      const response = await authenticatedFetch(`${API_BASE}/materials/project/${projectId}`, { method: "GET" });
      if (!response.ok) throw new Error(await readError(response, "Unable to load materials"));
      const data = await response.json();
      setMaterials(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error(error);
      if (!error.message.includes("Authentication failed")) setGlobalError(error.message);
    } finally {
      setLoading(false);
    }
  };

  const uploadMaterial = async (projectId, file) => {
    if (!projectId) {
      setGlobalError("Select a project before uploading a file.");
      return null;
    }
    if (!file) {
      setGlobalError("Please select a PDF or TXT file.");
      return null;
    }

    const name = file.name.toLowerCase();
    const valid = name.endsWith(".pdf") || name.endsWith(".txt");
    if (!valid) {
      setGlobalError("Only PDF and TXT files are supported.");
      return null;
    }

    try {
      setLoading(true);
      clearMessages();
      const formData = new FormData();
      formData.append("file", file);

      const response = await authenticatedFetch(
        `${API_BASE}/materials/project/${projectId}/upload`,
        { method: "POST", body: formData }
      );

      if (!response.ok) throw new Error(await readError(response, "Upload failed"));
      const material = await response.json();
      setGlobalMessage(`${file.name} uploaded successfully.`);
      await loadMaterials(projectId);
      return material;
    } catch (error) {
      console.error("Upload error:", error);
      if (!error.message.includes("Authentication failed")) setGlobalError(error.message);
      return null;
    } finally {
      setLoading(false);
    }
  };

  const deleteMaterial = async (materialId) => {
    if (!window.confirm("Delete this study material?")) return;

    try {
      setLoading(true);
      clearMessages();
      const response = await authenticatedFetch(`${API_BASE}/materials/${materialId}`, { method: "DELETE" });
      if (!response.ok) throw new Error(await readError(response, "Unable to delete material"));
      setMaterials((current) => current.filter((item) => String(item.id) !== String(materialId)));
      setGlobalMessage("Material deleted.");
    } catch (error) {
      console.error(error);
      if (!error.message.includes("Authentication failed")) setGlobalError(error.message);
    } finally {
      setLoading(false);
    }
  };

  /* =========================================================
     MATERIAL AI: SUMMARY / EXPLAIN / QUIZ
     ========================================================= */
  const materialAiAction = async (materialId, action) => {
    try {
      setLoading(true);
      clearMessages();
      setAiResult("");
      setAiResultTitle("");

      const response = await authenticatedFetch(`${API_BASE}/materials/${materialId}/${action}`, {
        method: "POST",
      });

      if (!response.ok) throw new Error(await readError(response, `AI ${action} failed`));
      const result = await response.text();
      setAiResult(result || "The AI returned an empty response.");
      setAiResultTitle(action === "summarize" ? "AI Summary" : action === "explain" ? "AI Explanation" : "Adaptive Quiz");
      return result;
    } catch (error) {
      console.error(`AI ${action} error:`, error);
      if (!error.message.includes("Authentication failed")) setGlobalError(error.message);
      return null;
    } finally {
      setLoading(false);
    }
  };

  /* =========================================================
     AI TUTOR
     ========================================================= */
  const askAi = async (question, project, material) => {
    if (!question.trim()) throw new Error("Please type a question.");
    if (!project?.id) throw new Error("Please select a project first.");
    if (!material?.id) throw new Error("Please select a study material first.");

    const response = await authenticatedFetch(`${API_BASE}/ai/ask`, {
      method: "POST",
      body: JSON.stringify({
        message: question.trim(),
        projectId: project.id,
        materialId: material.id,
      }),
    });

    if (!response.ok) throw new Error(await readError(response, "AI request failed"));
    const data = await response.json();
    return data.answer || data.message || data.content || "No AI answer was returned.";
  };

  /* =========================================================
     INITIAL LOAD
     ========================================================= */
  useEffect(() => {
    if (!token) return;
    loadSpaces();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  useEffect(() => {
    if (selectedSpace?.id && token) loadProjects(selectedSpace.id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedSpace?.id, token]);

  useEffect(() => {
    if (selectedProject?.id && token) loadMaterials(selectedProject.id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedProject?.id, token]);

  if (!token || page === "login") {
    return (
      <>
        <LoginPage
          error={globalError}
          onLogin={(data) => {
            localStorage.setItem("token", data.token);
            localStorage.setItem("email", data.email || "");
            if (data.userId != null) localStorage.setItem("userId", String(data.userId));
            setToken(data.token);
            setEmail(data.email || "");
            setGlobalError("");
            window.history.replaceState({}, "", "/dashboard");
            setPage("dashboard");
          }}
        />
        <GlobalStyles />
      </>
    );
  }

  const shared = {
    email,
    navigate,
    logout,
    message: globalMessage,
    error: globalError,
    loading,
  };

  if (page === "dashboard") {
    return (
      <Layout {...shared}>
        <DashboardPage spaces={spaces} projects={projects} materials={materials} navigate={navigate} selectedProject={selectedProject} />
        <GlobalStyles />
      </Layout>
    );
  }

  if (page === "spaces") {
    return (
      <Layout {...shared}>
        <SpacesPage
          spaces={spaces}
          loading={loading}
          message={globalMessage}
          error={globalError}
          onCreate={createSpace}
          onDelete={deleteSpace}
          onOpen={(space) => {
            setSelectedSpace(space);
            saveSelection("selectedSpace", space);
            setSelectedProject(null);
            saveSelection("selectedProject", null);
            navigate("projects");
          }}
        />
        <GlobalStyles />
      </Layout>
    );
  }

  if (page === "projects") {
    return (
      <Layout {...shared}>
        <ProjectsPage
          spaces={spaces}
          selectedSpace={selectedSpace}
          projects={projects}
          loading={loading}
          message={globalMessage}
          error={globalError}
          onSelectSpace={(space) => {
            setSelectedSpace(space);
            saveSelection("selectedSpace", space);
            setSelectedProject(null);
            saveSelection("selectedProject", null);
            setMaterials([]);
            loadProjects(space.id);
          }}
          onCreate={createProject}
          onDelete={deleteProject}
          onOpen={(project) => {
            setSelectedProject(project);
            saveSelection("selectedProject", project);
            localStorage.setItem("selectedProjectId", String(project.id));
            loadMaterials(project.id);
            navigate("materials");
          }}
        />
        <GlobalStyles />
      </Layout>
    );
  }

  if (page === "materials") {
    return (
      <Layout {...shared}>
        <MaterialsPage
          project={selectedProject}
          materials={materials}
          loading={loading}
          message={globalMessage}
          error={globalError}
          onUpload={uploadMaterial}
          onDelete={deleteMaterial}
          onAiAction={materialAiAction}
          onOpenChat={() => navigate("ai-chat")}
        />
        <GlobalStyles />
      </Layout>
    );
  }

  if (page === "ai-chat") {
    return (
      <Layout {...shared}>
        <AiChatPage
          projects={projects}
          selectedProject={selectedProject}
          setSelectedProject={(project) => {
            setSelectedProject(project);
            saveSelection("selectedProject", project);
            if (project) {
              loadMaterials(project.id);
            } else {
              setMaterials([]);
            }
          }}
          materials={materials}
          loading={loading}
          uploadMaterial={uploadMaterial}
          askAi={askAi}
          materialAiAction={materialAiAction}
          onDeleteMaterial={deleteMaterial}
          onOpenMaterials={() => navigate("materials")}
          onOpenQuiz={(material) => {
            setSelectedProject((current) => current || selectedProject);
            navigate("quiz");
            materialAiAction(material.id, "quiz");
          }}
          initialResult={aiResult}
          initialResultTitle={aiResultTitle}
          clearResult={() => {
            setAiResult("");
            setAiResultTitle("");
          }}
        />
        <GlobalStyles />
      </Layout>
    );
  }

  if (page === "quiz") {
    return (
      <Layout {...shared}>
        <QuizPage
          project={selectedProject}
          materials={materials}
          loading={loading}
          result={aiResult}
          resultTitle={aiResultTitle}
          onGenerate={async (material) => {
            const result = await materialAiAction(material.id, "quiz");
            if (result) {
              setAiResult(result);
              setAiResultTitle("Adaptive Quiz");
            }
          }}
          onOpenChat={() => navigate("ai-chat")}
        />
        <GlobalStyles />
      </Layout>
    );
  }

  return (
    <Layout {...shared}>
      <EmptyState
        icon="❓"
        title="Page not found"
        text="The requested page does not exist."
        action="Go to Dashboard"
        onAction={() => navigate("dashboard")}
      />
      <GlobalStyles />
    </Layout>
  );
}

/* =============================================================
   LOGIN
   ============================================================= */
function LoginPage({ onLogin, error: externalError }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const login = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError("");

    try {
      const response = await fetch(`${API_BASE}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: email.trim(), password }),
      });

      if (!response.ok) {
        throw new Error(await readLoginError(response));
      }

      const data = await response.json();
      if (!data.token) throw new Error("Login succeeded but the backend did not return a JWT token.");

      onLogin({
        token: data.token,
        email: data.email || email.trim(),
        userId: data.userId ?? data.id ?? data.user?.id ?? null,
      });
    } catch (err) {
      console.error("Login error:", err);
      setError(err.message || "Unable to login.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-logo">🤖</div>
        <div className="eyebrow">AI POWERED LEARNING</div>
        <h1>AI Study Companion</h1>
        <p>Login to organize your materials, chat with your AI tutor and practice with quizzes.</p>

        {(error || externalError) && <div className="error">{error || externalError}</div>}

        <form onSubmit={login}>
          <label>Email</label>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="Enter your email" required />
          <label>Password</label>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Enter your password" required />
          <button className="primary-button full-button" disabled={loading} type="submit">
            {loading ? "Logging in..." : "Login"}
          </button>
        </form>
      </div>
    </div>
  );
}

async function readLoginError(response) {
  try {
    const text = await response.text();
    return text || `Login failed (${response.status})`;
  } catch {
    return `Login failed (${response.status})`;
  }
}

/* =============================================================
   LAYOUT / NAVIGATION
   ============================================================= */
function Layout({ children, email, navigate, logout, message, error }) {
  return (
    <div className="app-shell">
      <header className="navbar">
        <button className="brand-button" onClick={() => navigate("dashboard")}>AI Study Companion</button>
        <nav>
          <button className="nav-button" onClick={() => navigate("dashboard")}>Dashboard</button>
          <button className="nav-button" onClick={() => navigate("spaces")}>Spaces</button>
          <button className="nav-button" onClick={() => navigate("projects")}>Projects</button>
          <button className="nav-button ai-nav-button" onClick={() => navigate("ai-chat")}>🤖 AI Tutor</button>
          <button className="nav-button quiz-nav-button" onClick={() => navigate("quiz")}>📝 Quiz</button>
          <button className="logout-button" onClick={logout}>Logout</button>
        </nav>
      </header>
      <div className="user-bar">Logged in as <strong>{email}</strong></div>
      <main>
        {message && <div className="global-message success">{message}</div>}
        {error && <div className="global-message error">{error}</div>}
        {children}
      </main>
    </div>
  );
}

/* =============================================================
   DASHBOARD
   ============================================================= */
function DashboardPage({ spaces, projects, materials, navigate, selectedProject }) {
  return (
    <div className="page dashboard-page">
      <section className="dashboard-hero">
        <div className="eyebrow">YOUR PERSONAL LEARNING WORKSPACE</div>
        <h1>Learn smarter with your AI Study Companion</h1>
        <p>Create a space, create a project, upload a PDF, then ask questions and test your understanding.</p>
      </section>

      <section className="stats-grid">
        <button className="stat-card" onClick={() => navigate("spaces")}>
          <span className="stat-icon">📚</span><strong>{spaces.length}</strong><span>Study Spaces</span>
        </button>
        <button className="stat-card" onClick={() => navigate("projects")}>
          <span className="stat-icon">📁</span><strong>{projects.length}</strong><span>Projects</span>
        </button>
        <button className="stat-card" onClick={() => selectedProject ? navigate("materials") : navigate("projects")}>
          <span className="stat-icon">📄</span><strong>{materials.length}</strong><span>Current Materials</span>
        </button>
        <button className="stat-card ai-stat" onClick={() => navigate("ai-chat")}>
          <span className="stat-icon">🤖</span><strong>AI</strong><span>Tutor & Quiz</span>
        </button>
      </section>

      <section className="learning-loop">
        <h2>Complete Learning Loop</h2>
        <div className="flow-grid">
          <FlowStep n="1" icon="📚" title="Space" text="Organize a subject" />
          <FlowStep n="2" icon="📁" title="Project" text="Choose what to learn" />
          <FlowStep n="3" icon="📄" title="PDF" text="Upload study material" />
          <FlowStep n="4" icon="🤖" title="AI Tutor" text="Ask grounded questions" />
          <FlowStep n="5" icon="📝" title="Quiz" text="Check understanding" />
        </div>
      </section>

      <section className="dashboard-actions">
        <div>
          <h2>Start studying</h2>
          <p>If you are starting from zero, create your first study space.</p>
        </div>
        <button className="primary-button" onClick={() => navigate(spaces.length ? "projects" : "spaces")}>
          {spaces.length ? "Continue to Projects" : "Create Study Space"}
        </button>
      </section>
    </div>
  );
}

function FlowStep({ n, icon, title, text }) {
  return <div className="flow-step"><span className="flow-number">{n}</span><span className="flow-icon">{icon}</span><div><strong>{title}</strong><small>{text}</small></div></div>;
}

/* =============================================================
   SPACES
   ============================================================= */
function SpacesPage({ spaces, loading, message, error, onCreate, onDelete, onOpen }) {
  const [showForm, setShowForm] = useState(false);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");

  const submit = async (event) => {
    event.preventDefault();
    const created = await onCreate(name, description);
    if (created) {
      setName("");
      setDescription("");
      setShowForm(false);
    }
  };

  return (
    <div className="page">
      <PageHeader title="Study Spaces" subtitle="Create separate learning environments for your subjects." buttonText="+ Create Space" onButton={() => setShowForm((v) => !v)} />
      {message && <div className="success">{message}</div>}
      {error && <div className="error">{error}</div>}

      {showForm && (
        <div className="form-card">
          <h2>Create Study Space</h2>
          <form onSubmit={submit}>
            <label>Space Name</label>
            <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Example: Java Backend" required />
            <label>Description</label>
            <textarea value={description} onChange={(e) => setDescription(e.target.value)} placeholder="What are you studying in this space?" />
            <div className="form-actions">
              <button className="primary-button" type="submit" disabled={loading}>{loading ? "Creating..." : "Create Space"}</button>
              <button className="secondary-button" type="button" onClick={() => setShowForm(false)}>Cancel</button>
            </div>
          </form>
        </div>
      )}

      {spaces.length === 0 ? (
        <EmptyState icon="📂" title="No study spaces yet" text="Create a study space to start the learning flow." action="Create Space" onAction={() => setShowForm(true)} />
      ) : (
        <div className="card-grid">
          {spaces.map((space) => (
            <div className="space-card" key={space.id}>
              <div className="large-icon">📚</div>
              <h2>{space.name}</h2>
              <p>{space.description || "No description added."}</p>
              <div className="card-footer">
                <button className="primary-small-button" onClick={() => onOpen(space)}>Open Space</button>
                <button className="delete-button" onClick={() => onDelete(space.id)}>Delete</button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

/* =============================================================
   PROJECTS
   ============================================================= */
function ProjectsPage({ spaces, selectedSpace, projects, loading, message, error, onSelectSpace, onCreate, onDelete, onOpen }) {
  const [showForm, setShowForm] = useState(false);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [learningGoal, setLearningGoal] = useState("");

  const submit = async (event) => {
    event.preventDefault();
    const created = await onCreate(selectedSpace?.id, name, description, learningGoal);
    if (created) {
      setName(""); setDescription(""); setLearningGoal(""); setShowForm(false);
    }
  };

  return (
    <div className="page">
      <PageHeader title="Projects" subtitle="Projects belong to a study space and contain your learning materials." buttonText="+ Create Project" onButton={() => setShowForm((v) => !v)} />
      {message && <div className="success">{message}</div>}
      {error && <div className="error">{error}</div>}

      <div className="selector-card">
        <label>Select Study Space</label>
        <select value={selectedSpace?.id || ""} onChange={(e) => {
          const space = spaces.find((item) => String(item.id) === e.target.value);
          if (space) onSelectSpace(space);
        }}>
          <option value="">-- Select a Study Space --</option>
          {spaces.map((space) => <option key={space.id} value={space.id}>{space.name}</option>)}
        </select>
        {!spaces.length && <p className="hint">No spaces exist yet. Go to Spaces and create one.</p>}
      </div>

      {showForm && (
        <div className="form-card">
          <h2>Create Project</h2>
          {!selectedSpace && <div className="warning">Select a study space before creating a project.</div>}
          <form onSubmit={submit}>
            <label>Project Name</label>
            <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Example: Spring Boot" required />
            <label>Description</label>
            <textarea value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Project description" />
            <label>Learning Goal</label>
            <textarea value={learningGoal} onChange={(e) => setLearningGoal(e.target.value)} placeholder="What should you learn?" />
            <div className="form-actions">
              <button className="primary-button" type="submit" disabled={loading || !selectedSpace}>{loading ? "Creating..." : "Create Project"}</button>
              <button className="secondary-button" type="button" onClick={() => setShowForm(false)}>Cancel</button>
            </div>
          </form>
        </div>
      )}

      {!selectedSpace ? (
        <EmptyState icon="📁" title="Select a study space" text="Projects are created inside a study space. Select one above." />
      ) : projects.length === 0 ? (
        <EmptyState icon="🗂️" title="No projects in this space" text={`Create the first project in ${selectedSpace.name}.`} action="Create Project" onAction={() => setShowForm(true)} />
      ) : (
        <div className="card-grid">
          {projects.map((project) => (
            <div className="project-card" key={project.id}>
              <div className="large-icon">📁</div>
              <h2>{project.name}</h2>
              <p>{project.description || "No description added."}</p>
              {project.learningGoal && <div className="goal"><strong>Learning Goal</strong><span>{project.learningGoal}</span></div>}
              <div className="card-footer">
                <button className="primary-small-button" onClick={() => onOpen(project)}>Open Project</button>
                <button className="delete-button" onClick={() => onDelete(project.id)}>Delete</button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

/* =============================================================
   MATERIALS
   ============================================================= */
function MaterialsPage({ project, materials, loading, message, error, onUpload, onDelete, onAiAction, onOpenChat }) {
  const [selectedFile, setSelectedFile] = useState(null);
  const [result, setResult] = useState("");
  const [resultTitle, setResultTitle] = useState("");

  if (!project) {
    return <div className="page"><EmptyState icon="📄" title="No project selected" text="Go to Projects, select a space and open a project." /></div>;
  }

  const upload = async () => {
    const material = await onUpload(project.id, selectedFile);
    if (material) setSelectedFile(null);
  };

  const runAi = async (material, action) => {
    const value = await onAiAction(material.id, action);
    if (value) {
      setResult(value);
      setResultTitle(action === "summarize" ? "AI Summary" : action === "explain" ? "AI Explanation" : "Adaptive Quiz");
    }
  };

  return (
    <div className="page">
      <div className="breadcrumb">Projects / {project.name}</div>
      <PageHeader title={project.name} subtitle="Upload your study materials and use AI actions directly on them." />
      {message && <div className="success">{message}</div>}
      {error && <div className="error">{error}</div>}

      <div className="upload-card">
        <div className="upload-icon">📄</div>
        <h2>Upload Study Material</h2>
        <p>PDF and TXT are supported. The backend extracts the content so the AI can use it.</p>
        <label className="file-drop-zone" htmlFor="material-upload">
          <span>📎</span>
          <strong>{selectedFile ? selectedFile.name : "Choose a PDF or TXT file"}</strong>
          <small>Click here to browse files</small>
        </label>
        <input id="material-upload" className="hidden-file-input" type="file" accept=".pdf,.txt,application/pdf,text/plain" onChange={(e) => setSelectedFile(e.target.files?.[0] || null)} />
        <button className="primary-button" onClick={upload} disabled={loading || !selectedFile}>{loading ? "Uploading..." : "Upload Material"}</button>
      </div>

      {result && (
        <AiResult title={resultTitle} result={result} onClose={() => { setResult(""); setResultTitle(""); }} />
      )}

      <div className="section-heading-row"><div><h2>Uploaded Materials</h2><p>{materials.length} material{materials.length === 1 ? "" : "s"}</p></div></div>

      {materials.length === 0 ? (
        <EmptyState icon="📕" title="No materials yet" text="Upload a PDF to enable Summary, Explain, Quiz and material-aware AI Tutor chat." />
      ) : (
        <div className="material-list">
          {materials.map((material) => (
            <MaterialCard key={material.id} material={material} loading={loading} onAi={runAi} onDelete={onDelete} />
          ))}
        </div>
      )}

      <div className="ai-continue-card">
        <div><span className="ai-continue-icon">🤖</span><div><h3>Ask the AI Tutor</h3><p>Open the chat and ask questions about your selected project material.</p></div></div>
        <button className="primary-button" onClick={onOpenChat}>Open AI Tutor</button>
      </div>
    </div>
  );
}

function MaterialCard({ material, loading, onAi, onDelete }) {
  const isPdf = String(material.fileType || material.fileName || "").toLowerCase().includes("pdf");
  return (
    <div className="material-card">
      <div className="material-icon">{isPdf ? "📕" : "📄"}</div>
      <div className="material-info">
        <h3>{material.fileName || material.title || "Study Material"}</h3>
        <p>{formatFileSize(material.fileSize)} {material.status ? `• ${material.status}` : ""}</p>
      </div>
      <div className="material-actions">
        <button className="ai-action-button" disabled={loading} onClick={() => onAi(material, "summarize")}>Summary</button>
        <button className="ai-action-button" disabled={loading} onClick={() => onAi(material, "explain")}>Explain</button>
        <button className="ai-action-button quiz-action" disabled={loading} onClick={() => onAi(material, "quiz")}>📝 Quiz</button>
        <button className="delete-button" onClick={() => onDelete(material.id)}>Delete</button>
      </div>
    </div>
  );
}

/* =============================================================
   AI CHAT
   ============================================================= */
function AiChatPage({ projects, selectedProject, setSelectedProject, materials, loading, uploadMaterial, askAi, materialAiAction, onDeleteMaterial, onOpenMaterials, initialResult, initialResultTitle, clearResult }) {
  const [messages, setMessages] = useState(() => [{ role: "assistant", content: "Hello! Select a project and a material, then ask me anything about your study material. I can also generate a quiz." }]);
  const [input, setInput] = useState("");
  const [selectedMaterialId, setSelectedMaterialId] = useState("");
  const [sending, setSending] = useState(false);
  const [file, setFile] = useState(null);
  const [localResult, setLocalResult] = useState(initialResult || "");
  const [localResultTitle, setLocalResultTitle] = useState(initialResultTitle || "");

  useEffect(() => {
    setLocalResult(initialResult || "");
    setLocalResultTitle(initialResultTitle || "");
  }, [initialResult, initialResultTitle]);

  useEffect(() => {
    if (materials.length && !materials.some((m) => String(m.id) === String(selectedMaterialId))) {
      setSelectedMaterialId(String(materials[0].id));
    }
    if (!materials.length) setSelectedMaterialId("");
  }, [materials, selectedMaterialId]);

  const selectedMaterial = materials.find((m) => String(m.id) === String(selectedMaterialId));

  const send = async () => {
    const question = input.trim();
    if (!question || sending) return;

    setMessages((current) => [...current, { role: "user", content: question }]);
    setInput("");
    setSending(true);

    try {
      const answer = await askAi(question, selectedProject, selectedMaterial);
      setMessages((current) => [...current, { role: "assistant", content: answer }]);
    } catch (error) {
      console.error(error);
      setMessages((current) => [...current, { role: "assistant", content: `Error: ${error.message}` }]);
    } finally {
      setSending(false);
    }
  };

  const handleUpload = async () => {
    if (!selectedProject || !file) return;
    const material = await uploadMaterial(selectedProject.id, file);
    if (material) {
      setFile(null);
      setSelectedMaterialId(String(material.id));
    }
  };

  const runAction = async (action) => {
    if (!selectedMaterial) return;
    const result = await materialAiAction(selectedMaterial.id, action);
    if (result) {
      setLocalResult(result);
      setLocalResultTitle(action === "summarize" ? "AI Summary" : action === "explain" ? "AI Explanation" : "Adaptive Quiz");
    }
  };

  return (
    <div className="page ai-chat-page">
      <div className="ai-chat-header">
        <div><div className="eyebrow">AI LEARNING PARTNER</div><h1>AI Tutor</h1><p>Ask questions using your selected study material as context.</p></div>
        <div className="ai-header-icon">🤖</div>
      </div>

      <div className="ai-workspace">
        <aside className="ai-sidebar">
          <div className="sidebar-section">
            <h3>1. Select Project</h3>
            <select value={selectedProject?.id || ""} onChange={(e) => {
              const project = projects.find((p) => String(p.id) === e.target.value);
              setSelectedProject(project || null);
            }}>
              <option value="">-- Select Project --</option>
              {projects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}
            </select>
            {!projects.length && <p className="hint">Create a project first.</p>}
          </div>

          <div className="sidebar-section">
            <h3>2. Upload PDF / TXT</h3>
            <label className="chat-file-picker" htmlFor="chat-upload"><span>📎</span><strong>{file ? file.name : "Choose file"}</strong><small>PDF or TXT</small></label>
            <input id="chat-upload" className="hidden-file-input" type="file" accept=".pdf,.txt,application/pdf,text/plain" onChange={(e) => setFile(e.target.files?.[0] || null)} />
            <button className="primary-button sidebar-upload-button" onClick={handleUpload} disabled={!selectedProject || !file || loading}>{loading ? "Uploading..." : "Upload Material"}</button>
          </div>

          <div className="sidebar-section">
            <h3>3. Select Material</h3>
            <select value={selectedMaterialId} onChange={(e) => setSelectedMaterialId(e.target.value)} disabled={!materials.length}>
              <option value="">-- Select Material --</option>
              {materials.map((material) => <option key={material.id} value={material.id}>{material.fileName || material.title || `Material ${material.id}`}</option>)}
            </select>
            {!materials.length && <p className="hint">Upload a material to enable grounded chat.</p>}
          </div>

          {selectedMaterial && (
            <div className="sidebar-section">
              <h3>4. Material AI</h3>
              <button className="side-action" onClick={() => runAction("summarize")}>📌 Summary</button>
              <button className="side-action" onClick={() => runAction("explain")}>💡 Explain</button>
              <button className="side-action quiz-side-action" onClick={() => runAction("quiz")}>📝 Generate Quiz</button>
              <button className="side-link" onClick={onOpenMaterials}>Manage Materials →</button>
            </div>
          )}
        </aside>

        <section className="chat-panel">
          <div className="chat-context-bar">
            <span>Project: <strong>{selectedProject?.name || "Not selected"}</strong></span>
            <span>Material: <strong>{selectedMaterial?.fileName || "Not selected"}</strong></span>
          </div>

          <div className="chat-messages">
            {messages.map((message, index) => (
              <div key={index} className={`chat-message ${message.role}`}>
                <div className="message-role">{message.role === "user" ? "You" : "AI Tutor"}</div>
                <div className="message-content">{message.content}</div>
              </div>
            ))}
            {sending && <div className="chat-message assistant"><div className="message-role">AI Tutor</div><div className="typing">Thinking...</div></div>}
          </div>

          <div className="chat-input-area">
            <textarea value={input} onChange={(e) => setInput(e.target.value)} onKeyDown={(e) => { if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); send(); } }} placeholder={selectedMaterial ? "Ask a question about this material..." : "Select a project and material first..."} disabled={sending || !selectedProject || !selectedMaterial} />
            <button className="primary-button send-button" onClick={send} disabled={sending || !input.trim() || !selectedProject || !selectedMaterial}>{sending ? "..." : "Send ➤"}</button>
          </div>
          <p className="chat-hint">Press Enter to send • Shift + Enter for a new line</p>
        </section>
      </div>

      {localResult && <AiResult title={localResultTitle} result={localResult} onClose={() => { setLocalResult(""); setLocalResultTitle(""); clearResult(); }} />}

      {selectedMaterial && <div className="chat-material-footer"><span>📄 {selectedMaterial.fileName}</span><button className="delete-button" onClick={() => onDeleteMaterial(selectedMaterial.id)}>Delete Material</button></div>}
    </div>
  );
}

/* =============================================================
   QUIZ PAGE
   ============================================================= */
function QuizPage({ project, materials, loading, result, resultTitle, onGenerate, onOpenChat }) {
  const [selectedId, setSelectedId] = useState("");
  const selected = materials.find((m) => String(m.id) === String(selectedId));

  useEffect(() => {
    if (!selectedId && materials.length) setSelectedId(String(materials[0].id));
  }, [materials, selectedId]);

  return (
    <div className="page quiz-page">
      <div className="quiz-hero"><div className="eyebrow">ASSESS YOUR UNDERSTANDING</div><h1>Adaptive Quiz</h1><p>Generate a quiz directly from the extracted content of your study material.</p></div>

      {!project ? (
        <EmptyState icon="📝" title="Select a project first" text="Open a project and upload a material before generating a quiz." />
      ) : materials.length === 0 ? (
        <EmptyState icon="📄" title="No materials available" text="Upload a PDF or TXT material before generating a quiz." />
      ) : (
        <>
          <div className="quiz-control-card">
            <label>Project</label><input value={project.name || ""} readOnly />
            <label>Material</label>
            <select value={selectedId} onChange={(e) => setSelectedId(e.target.value)}>
              {materials.map((m) => <option key={m.id} value={m.id}>{m.fileName || m.title || `Material ${m.id}`}</option>)}
            </select>
            <button className="primary-button" onClick={() => selected && onGenerate(selected)} disabled={!selected || loading}>{loading ? "Generating Quiz..." : "📝 Generate 5-Question Quiz"}</button>
          </div>

          {result && <AiResult title={resultTitle || "Adaptive Quiz"} result={result} onClose={() => {}} />}

          <div className="quiz-info-grid">
            <div><span>📌</span><strong>5 Questions</strong><small>Generated from your material</small></div>
            <div><span>🎯</span><strong>4 Options</strong><small>Multiple-choice practice</small></div>
            <div><span>💡</span><strong>Explanations</strong><small>Review why an answer is correct</small></div>
          </div>

          <div className="dashboard-actions"><div><h2>Want to discuss a question?</h2><p>Use the AI Tutor for explanations before or after the quiz.</p></div><button className="primary-button" onClick={onOpenChat}>Open AI Tutor</button></div>
        </>
      )}
    </div>
  );
}

/* =============================================================
   SHARED COMPONENTS
   ============================================================= */
function PageHeader({ title, subtitle, buttonText, onButton }) {
  return <div className="page-header"><div><h1>{title}</h1>{subtitle && <p className="subtitle">{subtitle}</p>}</div>{buttonText && <button className="primary-button" onClick={onButton}>{buttonText}</button>}</div>;
}

function EmptyState({ icon, title, text, action, onAction }) {
  return <div className="empty-box"><div className="empty-icon">{icon}</div><h2>{title}</h2><p>{text}</p>{action && <button className="primary-button" onClick={onAction}>{action}</button>}</div>;
}

function AiResult({ title, result, onClose }) {
  const isQuiz = title && title.toLowerCase().includes("quiz");

  return (
    <div className="ai-result-card">
      <div className="ai-result-header">
        <div>
          <span className="result-label">AI OUTPUT</span>
          <h2>{title}</h2>
        </div>
        <button className="close-result" onClick={onClose}>×</button>
      </div>

      {isQuiz
        ? <QuizResult result={result} />
        : <div className="ai-result-content">{result}</div>}
    </div>
  );
}

function QuizResult({ result }) {
  let parsed = null;
  try {
    const trimmed = String(result).trim();
    const start = trimmed.indexOf("{");
    const end = trimmed.lastIndexOf("}");
    if (start >= 0 && end > start) {
      parsed = JSON.parse(trimmed.slice(start, end + 1));
    }
  } catch {
    parsed = null;
  }

  if (!parsed || !Array.isArray(parsed.questions)) {
    return <div className="ai-result-content">{result}</div>;
  }

  return (
    <div className="quiz-result">
      {parsed.source && (
        <div className="quiz-source">Source: {parsed.source}</div>
      )}
      {parsed.questions.map((q, index) => (
        <div className="quiz-question" key={index}>
          <div className="quiz-question-header">
            <strong>Q{q.number || index + 1}.</strong>
            <span className="quiz-badge">{q.type}</span>
            {q.difficulty && <span className="quiz-difficulty">{q.difficulty}</span>}
            {q.concept && <span className="quiz-concept">{q.concept}</span>}
          </div>
          <div className="quiz-question-text">{q.question}</div>

          {Array.isArray(q.options) && q.options.length > 0 && (
            <ol className="quiz-options">
              {q.options.map((option, i) => (
                <li
                  key={i}
                  className={i === q.correctIndex ? "quiz-option correct" : "quiz-option"}
                >
                  {option}
                </li>
              ))}
            </ol>
          )}

          {q.expectedAnswer && (
            <div className="quiz-expected">
              <strong>Expected answer:</strong> {q.expectedAnswer}
            </div>
          )}

          {q.explanation && (
            <div className="quiz-explanation">
              <strong>Explanation:</strong> {q.explanation}
            </div>
          )}

          {q.citation && <div className="quiz-citation">{q.citation}</div>}
        </div>
      ))}
    </div>
  );
}

function formatFileSize(bytes) {
  if (!bytes) return "Size unavailable";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/* =============================================================
   GLOBAL STYLES
   ============================================================= */
function GlobalStyles() {
  return <style>{`
    * { box-sizing: border-box; }
    html, body, #root { min-height: 100%; }
    body { margin: 0; font-family: Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; background: #f5f7fb; color: #172033; }
    button, input, textarea, select { font: inherit; }
    button { cursor: pointer; }
    button:disabled { opacity: .58; cursor: not-allowed; transform: none !important; }
    .app-shell { min-height: 100vh; }
    .navbar { position: sticky; top: 0; z-index: 20; min-height: 74px; padding: 12px 42px; background: #fff; border-bottom: 1px solid #e5e7eb; display: flex; justify-content: space-between; align-items: center; gap: 20px; }
    .brand-button { border: 0; background: transparent; color: #7524ee; font-size: 21px; font-weight: 850; padding: 8px 0; }
    nav { display: flex; align-items: center; gap: 5px; flex-wrap: wrap; justify-content: flex-end; }
    .nav-button { border: 0; background: transparent; color: #344054; padding: 10px 12px; border-radius: 8px; font-weight: 650; }
    .nav-button:hover { background: #f3eaff; color: #6d20e8; }
    .ai-nav-button { color: #7020e8; }
    .quiz-nav-button { color: #b54708; }
    .logout-button { border: 1px solid #d9dce3; background: #fff; border-radius: 8px; padding: 9px 14px; font-weight: 650; margin-left: 6px; }
    .user-bar { background: #fafafa; border-bottom: 1px solid #eee; padding: 8px 42px; text-align: right; color: #667085; font-size: 12px; }
    main { min-height: calc(100vh - 108px); }
    .page { max-width: 1220px; margin: 0 auto; padding: 52px 28px 70px; }
    .page-header { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 30px; }
    .page h1 { margin: 0 0 8px; color: #111827; font-size: 40px; letter-spacing: -.8px; }
    .subtitle { color: #667085; margin: 0; font-size: 16px; line-height: 1.6; }
    .eyebrow { color: #7c22ff; font-size: 11px; font-weight: 850; letter-spacing: 1.6px; margin-bottom: 10px; }
    .primary-button { border: 0; border-radius: 9px; padding: 12px 19px; background: linear-gradient(135deg, #7020ed, #922dff); color: #fff; font-weight: 750; box-shadow: 0 5px 15px rgba(114,32,245,.17); transition: .18s; }
    .primary-button:hover { transform: translateY(-1px); box-shadow: 0 9px 22px rgba(114,32,245,.23); }
    .full-button { width: 100%; margin-top: 10px; }
    .primary-small-button { border: 0; background: #7623ed; color: #fff; padding: 9px 14px; border-radius: 7px; font-weight: 700; }
    .secondary-button { border: 0; background: #eef0f4; color: #344054; padding: 12px 19px; border-radius: 9px; font-weight: 700; }
    .success, .error, .warning { padding: 12px 15px; border-radius: 9px; margin-bottom: 18px; line-height: 1.5; }
    .success { background: #ecfdf3; color: #027a48; border: 1px solid #abefc6; }
    .error { background: #fef3f2; color: #b42318; border: 1px solid #fecdca; }
    .warning { background: #fffaeb; color: #b54708; border: 1px solid #fedf89; }
    .global-message { max-width: 1164px; margin: 20px auto 0; }
    .form-card, .selector-card, .upload-card, .quiz-control-card { background: #fff; border: 1px solid #e2e5eb; border-radius: 16px; padding: 28px; margin-bottom: 26px; box-shadow: 0 5px 20px rgba(16,24,40,.035); }
    .form-card h2, .upload-card h2 { margin-top: 0; }
    form { display: flex; flex-direction: column; gap: 9px; }
    label { font-weight: 700; font-size: 14px; margin-top: 6px; }
    input, textarea, select { width: 100%; padding: 12px 14px; border: 1px solid #d5d9e2; border-radius: 8px; outline: none; background: #fff; color: #172033; font-size: 15px; }
    input:focus, textarea:focus, select:focus { border-color: #7c22ff; box-shadow: 0 0 0 3px rgba(124,34,255,.1); }
    textarea { min-height: 100px; resize: vertical; }
    .form-actions { display: flex; gap: 10px; margin-top: 12px; }
    .selector-card label { display: block; margin-bottom: 10px; }
    .hint { color: #667085; font-size: 13px; }
    .card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }
    .space-card, .project-card { background: #fff; border: 1px solid #e2e5eb; border-radius: 16px; padding: 24px; box-shadow: 0 5px 20px rgba(16,24,40,.04); transition: .18s; }
    .space-card:hover, .project-card:hover { transform: translateY(-3px); box-shadow: 0 13px 30px rgba(16,24,40,.09); border-color: #d8c4ff; }
    .large-icon { font-size: 39px; }
    .space-card h2, .project-card h2 { margin: 12px 0 8px; }
    .space-card p, .project-card p { min-height: 45px; color: #667085; line-height: 1.5; }
    .card-footer { display: flex; gap: 9px; margin-top: 18px; flex-wrap: wrap; }
    .delete-button { border: 1px solid #ffd0cc; background: #fff0f0 !important; color: #d92d20 !important; padding: 8px 12px; border-radius: 7px; font-weight: 700; }
    .goal { background: #f7f2ff; border-radius: 9px; padding: 11px; font-size: 13px; color: #4a1b78; }
    .goal strong { display: block; margin-bottom: 4px; }
    .empty-box { background: #fff; border: 1px solid #e2e5eb; border-radius: 18px; padding: 65px 28px; text-align: center; margin-top: 24px; }
    .empty-icon { font-size: 53px; margin-bottom: 12px; }
    .empty-box h2 { margin: 0 0 8px; }
    .empty-box p { color: #667085; max-width: 650px; margin: 0 auto 20px; line-height: 1.6; }
    .dashboard-page { padding-top: 65px; }
    .dashboard-hero { text-align: center; max-width: 830px; margin: 0 auto 46px; }
    .dashboard-hero h1 { font-size: 46px; margin-bottom: 14px; }
    .dashboard-hero p { color: #667085; font-size: 18px; line-height: 1.65; }
    .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px; }
    .stat-card { border: 1px solid #e3e6ed; border-radius: 16px; background: #fff; padding: 24px 18px; display: flex; flex-direction: column; align-items: center; gap: 6px; color: #172033; transition: .18s; }
    .stat-card:hover { transform: translateY(-3px); border-color: #d8c4ff; box-shadow: 0 12px 28px rgba(124,34,255,.1); }
    .stat-icon { font-size: 34px; }
    .stat-card strong { font-size: 29px; }
    .stat-card span:last-child { color: #667085; font-size: 13px; font-weight: 650; }
    .learning-loop { margin-top: 34px; }
    .learning-loop h2 { text-align: center; margin-bottom: 18px; }
    .flow-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 10px; }
    .flow-step { background: #fff; border: 1px solid #e3e6ed; border-radius: 12px; padding: 15px 12px; display: flex; align-items: center; gap: 8px; position: relative; }
    .flow-number { background: #f1e8ff; color: #7c22ff; width: 25px; height: 25px; border-radius: 50%; display: grid; place-items: center; font-size: 12px; font-weight: 800; flex: 0 0 auto; }
    .flow-icon { font-size: 25px; }
    .flow-step strong, .flow-step small { display: block; }
    .flow-step small { color: #667085; margin-top: 3px; font-size: 11px; }
    .dashboard-actions, .ai-continue-card { background: linear-gradient(135deg, #faf7ff, #fff); border: 1px solid #dfceff; border-radius: 16px; padding: 25px; margin-top: 30px; display: flex; align-items: center; justify-content: space-between; gap: 20px; }
    .dashboard-actions h2, .ai-continue-card h3 { margin: 0 0 5px; }
    .dashboard-actions p, .ai-continue-card p { color: #667085; margin: 0; line-height: 1.5; }
    .upload-card p { color: #667085; }
    .upload-icon { font-size: 44px; }
    .file-drop-zone, .chat-file-picker { border: 2px dashed #d8c4ff; background: #fbf9ff; border-radius: 13px; cursor: pointer; }
    .file-drop-zone { padding: 27px; margin: 18px 0; display: flex; flex-direction: column; align-items: center; gap: 6px; text-align: center; }
    .file-drop-zone span, .chat-file-picker span { font-size: 30px; }
    .file-drop-zone small, .chat-file-picker small { color: #667085; }
    .hidden-file-input { display: none; }
    .breadcrumb { color: #7524ee; font-size: 13px; font-weight: 750; margin-bottom: 8px; }
    .section-heading-row { display: flex; justify-content: space-between; align-items: center; margin: 30px 0 15px; }
    .section-heading-row h2 { margin: 0 0 4px; }
    .section-heading-row p { margin: 0; color: #667085; font-size: 13px; }
    .material-list { display: flex; flex-direction: column; gap: 13px; }
    .material-card { background: #fff; border: 1px solid #e2e5eb; border-radius: 13px; padding: 17px 19px; display: flex; align-items: center; gap: 15px; }
    .material-icon { font-size: 34px; }
    .material-info { flex: 1; min-width: 0; }
    .material-info h3 { margin: 0 0 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .material-info p { margin: 0; color: #667085; font-size: 12px; }
    .material-actions { display: flex; align-items: center; justify-content: flex-end; gap: 7px; flex-wrap: wrap; }
    .ai-action-button { border: 1px solid #dfceff; background: #f4edff; color: #6d20e8; padding: 8px 11px; border-radius: 7px; font-size: 12px; font-weight: 750; }
    .quiz-action { background: #fff7e8; border-color: #fedf89; color: #b54708; }
    .ai-result-card { background: #fff; border: 1px solid #d9c2ff; border-radius: 17px; padding: 24px; margin: 25px 0; box-shadow: 0 10px 30px rgba(124,34,255,.08); }
    .ai-result-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 15px; margin-bottom: 17px; }
    .ai-result-header h2 { margin: 3px 0 0; }
    .result-label { color: #7c22ff; font-size: 10px; font-weight: 850; letter-spacing: 1px; }
    .close-result { border: 0; background: #f2f4f7; width: 32px; height: 32px; border-radius: 50%; font-size: 20px; }
    .ai-result-content { white-space: pre-wrap; color: #344054; line-height: 1.7; font-size: 15px; }
    .ai-continue-icon { font-size: 40px; }
    .ai-continue-card > div { display: flex; align-items: center; gap: 14px; }
    .ai-chat-page { max-width: 1320px; }
    .ai-chat-header { background: #fff; border: 1px solid #e2e5eb; border-radius: 19px; padding: 28px 32px; margin-bottom: 18px; display: flex; justify-content: space-between; align-items: center; }
    .ai-chat-header h1 { margin: 0 0 7px; font-size: 38px; }
    .ai-chat-header p { margin: 0; color: #667085; }
    .ai-header-icon { width: 90px; height: 90px; border-radius: 24px; display: grid; place-items: center; background: #f5edff; font-size: 57px; }
    .ai-workspace { min-height: 690px; background: #fff; border: 1px solid #e0e3e8; border-radius: 19px; overflow: hidden; display: grid; grid-template-columns: 315px 1fr; box-shadow: 0 10px 35px rgba(16,24,40,.06); }
    .ai-sidebar { background: #faf9fd; border-right: 1px solid #e4e7ec; padding: 22px; overflow-y: auto; }
    .sidebar-section { padding-bottom: 21px; margin-bottom: 21px; border-bottom: 1px solid #e5e7eb; }
    .sidebar-section:last-child { border-bottom: 0; }
    .sidebar-section h3 { font-size: 13px; margin: 0 0 11px; }
    .chat-file-picker { padding: 15px 9px; display: flex; flex-direction: column; align-items: center; text-align: center; gap: 5px; }
    .chat-file-picker strong { max-width: 235px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; }
    .sidebar-upload-button { width: 100%; margin-top: 9px; }
    .side-action { width: 100%; border: 1px solid #dfceff; background: #fff; color: #6d20e8; padding: 9px 10px; border-radius: 7px; font-weight: 700; text-align: left; margin-bottom: 7px; }
    .quiz-side-action { color: #b54708; border-color: #fedf89; background: #fffaf0; }
    .side-link { border: 0; background: transparent; color: #7524ee; font-weight: 700; padding: 5px 0; }
    .chat-panel { min-width: 0; display: flex; flex-direction: column; }
    .chat-context-bar { padding: 13px 18px; border-bottom: 1px solid #eee; background: #fcfcfd; display: flex; gap: 20px; flex-wrap: wrap; color: #667085; font-size: 12px; }
    .chat-messages { flex: 1; min-height: 500px; max-height: 550px; overflow-y: auto; padding: 22px; background: #fff; }
    .chat-message { max-width: 84%; padding: 12px 15px; border-radius: 13px; margin-bottom: 13px; line-height: 1.65; }
    .chat-message.user { margin-left: auto; background: #7623ed; color: #fff; border-bottom-right-radius: 4px; }
    .chat-message.assistant { background: #f4edff; color: #351052; border-bottom-left-radius: 4px; }
    .message-role { font-size: 10px; font-weight: 850; letter-spacing: .6px; opacity: .72; margin-bottom: 4px; }
    .message-content { white-space: pre-wrap; word-break: break-word; }
    .typing { color: #667085; }
    .chat-input-area { border-top: 1px solid #e5e7eb; padding: 15px; display: flex; gap: 10px; align-items: flex-end; }
    .chat-input-area textarea { min-height: 55px; max-height: 150px; }
    .send-button { height: 55px; min-width: 100px; }
    .chat-hint { margin: 0; padding: 0 15px 13px; color: #98a2b3; font-size: 11px; }
    .chat-material-footer { margin-top: 15px; background: #fff; border: 1px solid #e2e5eb; border-radius: 11px; padding: 12px 15px; display: flex; justify-content: space-between; align-items: center; gap: 10px; }
    .quiz-hero { text-align: center; max-width: 750px; margin: 0 auto 35px; }
    .quiz-hero h1 { margin-bottom: 12px; }
    .quiz-hero p { color: #667085; line-height: 1.6; }
    .quiz-control-card { max-width: 760px; margin: 0 auto; display: flex; flex-direction: column; gap: 10px; }
    .quiz-control-card button { margin-top: 8px; }
    .quiz-info-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 15px; margin-top: 25px; }
    .quiz-info-grid > div { background: #fff; border: 1px solid #e3e6ed; border-radius: 13px; padding: 18px; display: flex; flex-direction: column; gap: 5px; }
    .quiz-info-grid span { font-size: 25px; }
    .quiz-info-grid small { color: #667085; }
    .login-page { min-height: 100vh; display: grid; place-items: center; padding: 25px; background: radial-gradient(circle at top left, #e7edff, transparent 43%), radial-gradient(circle at bottom right, #f0e5ff, transparent 43%), #f5f7fb; }
    .login-card { width: 430px; max-width: 100%; background: #fff; border-radius: 21px; padding: 39px; box-shadow: 0 20px 55px rgba(31,41,55,.12); }
    .login-logo { font-size: 52px; text-align: center; }
    .login-card .eyebrow { text-align: center; margin-top: 8px; }
    .login-card h1 { text-align: center; margin: 7px 0 9px; font-size: 29px; }
    .login-card > p { text-align: center; color: #667085; line-height: 1.55; margin-bottom: 27px; }
    .login-card form { gap: 8px; }
    .login-card input { margin-bottom: 8px; }
    .quiz-result { display: flex; flex-direction: column; gap: 16px; text-align: left; }
    .quiz-source { font-weight: 700; color: #6d20e8; font-size: 13px; }
    .quiz-question { background: #faf7ff; border: 1px solid #e6d8ff; border-radius: 12px; padding: 16px; }
    .quiz-question-header { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-bottom: 8px; }
    .quiz-badge { background: #7623ed; color: #fff; font-size: 10px; font-weight: 800; padding: 3px 8px; border-radius: 20px; }
    .quiz-difficulty { background: #fff7e8; color: #b54708; font-size: 10px; font-weight: 800; padding: 3px 8px; border-radius: 20px; }
    .quiz-concept { background: #eef2ff; color: #3538cd; font-size: 10px; font-weight: 800; padding: 3px 8px; border-radius: 20px; }
    .quiz-question-text { font-weight: 600; margin-bottom: 10px; }
    .quiz-options { margin: 0 0 10px 18px; padding: 0; }
    .quiz-option { padding: 6px 10px; border-radius: 7px; margin-bottom: 4px; }
    .quiz-option.correct { background: #ecfdf3; color: #027a48; font-weight: 700; }
    .quiz-expected, .quiz-explanation { font-size: 13px; color: #344054; margin-top: 6px; line-height: 1.6; }
    .quiz-citation { font-size: 11px; color: #7c22ff; margin-top: 8px; font-weight: 700; }
    @media (max-width: 1000px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } .flow-grid { grid-template-columns: repeat(2, 1fr); } .ai-workspace { grid-template-columns: 270px 1fr; } }
    @media (max-width: 780px) { .navbar { padding: 14px 18px; flex-direction: column; align-items: stretch; } nav { justify-content: center; } .user-bar { padding: 7px 15px; text-align: center; } .page { padding: 35px 16px 55px; } .page h1, .dashboard-hero h1 { font-size: 32px; } .page-header, .dashboard-actions, .ai-continue-card { flex-direction: column; align-items: flex-start; } .stats-grid, .quiz-info-grid { grid-template-columns: 1fr; } .flow-grid { grid-template-columns: 1fr; } .ai-workspace { grid-template-columns: 1fr; } .ai-sidebar { border-right: 0; border-bottom: 1px solid #e4e7ec; } .chat-messages { max-height: 470px; } .material-card { align-items: flex-start; flex-wrap: wrap; } .material-info { width: calc(100% - 60px); } .material-actions { width: 100%; justify-content: flex-start; } .chat-input-area { flex-direction: column; align-items: stretch; } .send-button { width: 100%; } }
    @media (max-width: 500px) { .login-card { padding: 28px 21px; } .navbar nav { gap: 2px; } .nav-button { padding: 8px 7px; font-size: 12px; } .logout-button { font-size: 12px; } }
  `}</style>;
}

export default App;
