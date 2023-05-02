"""
Includes some utility functions
"""

def replace_in_model( model: str, **kwargs) -> str:
    """
    Replace the comment "Insert arg here" in the model by its value
    :param model: The base HTML model to use
    :param kwargs: Named args with the value being the one that is output
    :returns: The HTML code with comments removed and replaced
    """
    for key, value in kwargs.items():
        print(value)
        model = model.replace(f"<!-- Insert {key} here -->", value)
    return model
