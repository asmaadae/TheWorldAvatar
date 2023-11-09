from flask import Flask

import agent.instantiate as instantiate

app = Flask(__name__)
app.register_blueprint(instantiate.get_heat_data_bp)


if __name__ == "__main__":
    app.run()
