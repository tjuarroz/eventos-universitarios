"""Validación de JWT emitidos por el core Java (mismo secreto HMAC)."""
from dataclasses import dataclass

import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from .config import JWT_SECRET

_bearer = HTTPBearer(auto_error=False)


@dataclass
class Usuario:
    id: int
    email: str
    rol: str


def usuario_actual(cred: HTTPAuthorizationCredentials | None = Depends(_bearer)) -> Usuario:
    if cred is None:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Falta el token de acceso")
    try:
        datos = jwt.decode(cred.credentials, JWT_SECRET, algorithms=["HS256", "HS384", "HS512"])
    except jwt.PyJWTError:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Token inválido o expirado")
    return Usuario(id=int(datos.get("id", 0)), email=datos.get("sub", ""), rol=datos.get("rol", ""))


def requiere_rol(*roles: str):
    def _dep(u: Usuario = Depends(usuario_actual)) -> Usuario:
        if u.rol not in roles:
            raise HTTPException(status.HTTP_403_FORBIDDEN, "No tenés permisos para este recurso")
        return u
    return _dep
