﻿# Distributed Systems / Verteilte Systeme

![RepoSize](https://img.shields.io/github/repo-size/Smokey95/AIN_Distributed_Systems)
[![License](https://img.shields.io/github/license/Smokey95/AIN_Distributed_Systems)](https://cdn130.picsart.com/272563229032201.jpg?r1024x1024)
![Forks](https://img.shields.io/github/forks/Smokey95/AIN_Distributed_Systems?color=green&style=social)
![Watcher](https://img.shields.io/github/watchers/Smokey95/AIN_Distributed_Systems?style=social)

---

📄 This repository is intended for the lecture **Distributed Systems**, formally known as *Verteilte Systeme*

🗺️ The lecture is part of the study program AIN (Applied Computer Science) at University of Applied Sciences (HTWG) Konstanz.
 
---

## Tasks

Below you can find further information about tasks which contain different implementations

### Task 6

Topic of task 6 was the chapter `namespaces`. The task was to implement two diffrent version of location service. One with a **forwad-reference** and one with a **home-based** location service.

The **home-based** location service was merged into the master branch. You can find both versions in the branches:

- `06_task_namespaces_forward_references`
- `06_task_namespaces_homebased`

### Task 7

In Task 7 we had to implement a security feature that will encrypt the communication between the client and the server. Therfore it was necessary to implement two versions of this task. One with a **asysmmetric** and one with a **symmetric** encryption.

The task has shown, that the **asymmetric** encryption is much slower than the **symmetric** encryption. This is on one hand because the **asymmetric** encryption uses a public and a private key where the public key has to be shared but also because the **asymmetric** encryption used key length is typically much larger than the data being encrypted.

Therefore the **sysmmetric** encryption was merged into the master branch. You can find both versions in the branches:

- `07_task_asymmetric_encryption`
- `07_task_symmetric_encryption`

---

*The contributors involved assume no liability for the information provided. If you have any legal claims to the information shared or wish to report a violation, please contact smokey95.github@gmail.com*
