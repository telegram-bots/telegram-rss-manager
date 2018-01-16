from sqlalchemy.sql import text
from typing import Optional

from src.component.config import db
from src.domain.entities import User


class UserRepository:
    def get(self, telegram_id: int) -> Optional[User]:
        """
        Find user by his telegram_id
        :param telegram_id: User telegram_id
        :rtype: Optional[User]
        :return: Found user or None
        """
        return db.session \
            .query(User) \
            .from_statement(text("""SELECT * FROM users WHERE telegram_id = :telegram_id""")) \
            .params(telegram_id=telegram_id) \
            .first()

    def get_or_create(self, telegram_id: int) -> User:
        """
        Creates user if he doesn't exists or simply returns found user
        :param telegram_id: User telegram_id
        :rtype: User
        :return: Created or existing user
        """
        return db.session \
            .query(User) \
            .from_statement(text(
                """
                INSERT INTO users (telegram_id)
                VALUES (:telegram_id)
                ON CONFLICT DO UPDATE 
                  SET x = 1;
                  SET y = 2;
                SELECT * FROM users WHERE telegram_id = :telegram_id;
                """
            )) \
            .params(telegram_id=telegram_id) \
            .first()
