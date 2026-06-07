import os
import re

def calculate_progress():
    java_count = 0
    kt_count = 0
    
    for root, dirs, files in os.walk("app/src/main/java"):
        for file in files:
            if file.endswith(".java"):
                java_count += 1
            elif file.endswith(".kt"):
                kt_count += 1
                
    total = java_count + kt_count
    if total == 0:
        return 0
    # Calculate percentage
    percentage = (kt_count / total) * 100
    return round(percentage, 2)

def update_readme(progress):
    readme_path = "README.md"
    if not os.path.exists(readme_path):
        return
        
    with open(readme_path, "r", encoding="utf-8") as f:
        content = f.read()
        
    # Replace the Shields.io badge with new progress
    # Format: https://img.shields.io/badge/Progresso_Kotlin-XX.XX%25-blue
    new_badge = f"https://img.shields.io/badge/Progresso_Geral_Kotlin-{progress}%25-blue"
    updated_content = re.sub(
        r"https://img\.shields\.io/badge/Progresso_Geral_Kotlin-[\d\.]+%25-blue",
        new_badge,
        content
    )
    
    # Also update the progress bar inside the markdown
    # Format: **Progresso da Conversão:** [██░░░░░░░░] XX.XX%
    filled_blocks = int(progress // 10)
    empty_blocks = 10 - filled_blocks
    new_progress_bar = f"**Progresso da Conversão Geral:** [{'█' * filled_blocks}{'░' * empty_blocks}] {progress}%"
    updated_content = re.sub(
        r"\*\*Progresso da Conversão Geral:\*\* \[.*\] [\d\.]+%",
        new_progress_bar,
        updated_content
    )
    
    with open(readme_path, "w", encoding="utf-8") as f:
        f.write(updated_content)
        
    print(f"README.md atualizado com sucesso! Progresso: {progress}%")

if __name__ == "__main__":
    progress = calculate_progress()
    update_readme(progress)
