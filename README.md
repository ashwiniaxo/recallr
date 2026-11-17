# recallr

Le modèle Ollama utilisé pour générer et corriger les questions se configure avec
la variable d’environnement `OLLAMA_MODEL` avant de démarrer le backend.
Par défaut, Recallr utilise `qwen3:4b`. Le modèle choisi doit être installé dans
Ollama, accessible sur `http://localhost:11434`.
